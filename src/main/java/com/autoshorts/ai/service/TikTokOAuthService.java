package com.autoshorts.ai.service;

import com.autoshorts.ai.client.TikTokApiClient;
import com.autoshorts.ai.client.model.TikTokCreatorInfo;
import com.autoshorts.ai.client.model.TikTokTokenResponse;
import com.autoshorts.ai.config.AppProperties;
import com.autoshorts.ai.entity.TikTokAccountConnection;
import com.autoshorts.ai.entity.TikTokConnectionStatus;
import com.autoshorts.ai.exception.ExternalServiceException;
import com.autoshorts.ai.exception.InvalidJobStateException;
import com.autoshorts.ai.exception.ResourceNotFoundException;
import com.autoshorts.ai.repository.TikTokAccountConnectionRepository;
import com.autoshorts.ai.security.TikTokOAuthStateService;
import com.autoshorts.ai.security.TokenCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Drives the TikTok OAuth authorization-code flow and keeps stored access tokens fresh.
 */
@Service
public class TikTokOAuthService {

    private static final Logger log = LoggerFactory.getLogger(TikTokOAuthService.class);

    /** Refresh the access token when it expires within this window. */
    private static final long REFRESH_SKEW_SECONDS = 120;

    private final TikTokApiClient tikTokApiClient;
    private final TikTokOAuthStateService stateService;
    private final TikTokAccountConnectionRepository repository;
    private final ChannelService channelService;
    private final TokenCipher tokenCipher;
    private final AppProperties appProperties;

    public TikTokOAuthService(
        TikTokApiClient tikTokApiClient,
        TikTokOAuthStateService stateService,
        TikTokAccountConnectionRepository repository,
        ChannelService channelService,
        TokenCipher tokenCipher,
        AppProperties appProperties
    ) {
        this.tikTokApiClient = tikTokApiClient;
        this.stateService = stateService;
        this.repository = repository;
        this.channelService = channelService;
        this.tokenCipher = tokenCipher;
        this.appProperties = appProperties;
    }

    /** Build the TikTok authorize URL the frontend should redirect the user to. */
    public String buildAuthorizationUrl(UUID userId, UUID channelId) {
        AppProperties.TikTok cfg = appProperties.getTiktok();
        if (!StringUtils.hasText(cfg.getClientKey()) || !StringUtils.hasText(cfg.getRedirectUri())) {
            throw new InvalidJobStateException(
                "TikTok OAuth is not configured. Set TIKTOK_CLIENT_KEY and TIKTOK_REDIRECT_URI.");
        }
        UUID resolvedChannelId = channelService.resolveOwnedChannelIdOrDefault(userId, channelId);
        String state = stateService.createState(userId, resolvedChannelId);

        return trimTrailingSlash(cfg.getAuthBaseUrl()) + "/v2/auth/authorize/"
            + "?client_key=" + enc(cfg.getClientKey())
            + "&scope=" + enc(cfg.getScopes())
            + "&response_type=code"
            + "&redirect_uri=" + enc(cfg.getRedirectUri())
            + "&state=" + enc(state);
    }

    /**
     * Handle the OAuth redirect: verify state, exchange the code, and persist encrypted tokens.
     *
     * @return the absolute frontend URL to redirect the browser to (with a status query param)
     */
    public String handleCallback(String code, String state, String error) {
        if (StringUtils.hasText(error)) {
            log.warn("event=tiktok_oauth_denied error={}", error);
            return frontendReturn("error", error);
        }
        TikTokOAuthStateService.OAuthState parsed;
        try {
            parsed = stateService.parseState(state);
        } catch (Exception ex) {
            log.warn("event=tiktok_oauth_invalid_state message={}", ex.getMessage());
            return frontendReturn("error", "invalid_state");
        }
        try {
            persistTokens(parsed.userId(), parsed.channelId(), tikTokApiClient.exchangeAuthorizationCode(code));
            return frontendReturn("connected", null);
        } catch (Exception ex) {
            log.error("event=tiktok_oauth_exchange_failed userId={} message={}", parsed.userId(), ex.getMessage());
            return frontendReturn("error", "token_exchange_failed");
        }
    }

    /**
     * Return a currently-valid access token for the channel's active connection, refreshing it
     * transparently if it is expired or about to expire.
     */
    @Transactional
    public String getValidAccessToken(UUID userId, UUID channelId) {
        TikTokAccountConnection connection = repository.findByOwnerUserIdAndChannelId(userId, channelId)
            .orElseThrow(() -> new InvalidJobStateException("No TikTok connection for this channel"));
        if (connection.getStatus() != TikTokConnectionStatus.ACTIVE) {
            throw new InvalidJobStateException("TikTok connection is not active: " + connection.getStatus());
        }

        if (!isExpiringSoon(connection.getTokenExpiresAt())) {
            return tokenCipher.decrypt(connection.getAccessTokenEncrypted());
        }

        String refreshToken = tokenCipher.decrypt(connection.getRefreshTokenEncrypted());
        if (!StringUtils.hasText(refreshToken)) {
            connection.setStatus(TikTokConnectionStatus.EXPIRED);
            repository.save(connection);
            throw new InvalidJobStateException("TikTok access token expired and no refresh token is available; reconnect the account");
        }
        try {
            TikTokTokenResponse refreshed = tikTokApiClient.refreshAccessToken(refreshToken);
            applyTokens(connection, refreshed);
            repository.save(connection);
            log.info("event=tiktok_token_refreshed userId={} channelId={}", userId, channelId);
            return refreshed.accessToken();
        } catch (ExternalServiceException ex) {
            connection.setStatus(TikTokConnectionStatus.EXPIRED);
            repository.save(connection);
            throw new InvalidJobStateException("TikTok token refresh failed; reconnect the account: " + ex.getMessage());
        }
    }

    private void persistTokens(UUID userId, UUID channelId, TikTokTokenResponse tokens) {
        TikTokAccountConnection connection = repository.findByOwnerUserIdAndChannelId(userId, channelId)
            .orElseGet(TikTokAccountConnection::new);
        connection.setOwnerUserId(userId);
        connection.setChannelId(channelId);
        if (StringUtils.hasText(tokens.openId())) {
            connection.setPlatformAccountId(tokens.openId());
        }
        applyTokens(connection, tokens);
        connection.setStatus(TikTokConnectionStatus.ACTIVE);
        connection.setLastSyncAt(Instant.now());
        repository.save(connection);

        // Best-effort enrichment of the display username; never fail the connect flow on this.
        try {
            TikTokCreatorInfo info = tikTokApiClient.queryCreatorInfo(tokens.accessToken());
            if (StringUtils.hasText(info.creatorUsername())) {
                connection.setPlatformUsername(info.creatorUsername());
                repository.save(connection);
            }
        } catch (Exception ex) {
            log.warn("event=tiktok_creator_info_failed userId={} message={}", userId, ex.getMessage());
        }
        log.info("event=tiktok_oauth_connected userId={} channelId={} openId={}", userId, channelId, tokens.openId());
    }

    private void applyTokens(TikTokAccountConnection connection, TikTokTokenResponse tokens) {
        connection.setAccessTokenEncrypted(tokenCipher.encrypt(tokens.accessToken()));
        if (StringUtils.hasText(tokens.refreshToken())) {
            connection.setRefreshTokenEncrypted(tokenCipher.encrypt(tokens.refreshToken()));
        }
        connection.setTokenExpiresAt(tokens.accessTokenExpiresAt());
        if (tokens.scopes() != null && !tokens.scopes().isEmpty()) {
            connection.setScopes(String.join(",", tokens.scopes()));
        }
    }

    private boolean isExpiringSoon(Instant expiresAt) {
        return expiresAt == null || expiresAt.isBefore(Instant.now().plusSeconds(REFRESH_SKEW_SECONDS));
    }

    private String frontendReturn(String status, String detail) {
        String base = appProperties.getTiktok().getFrontendReturnUrl();
        String separator = base.contains("?") ? "&" : "?";
        String url = base + separator + "tiktok=" + enc(status);
        if (StringUtils.hasText(detail)) {
            url += "&detail=" + enc(detail);
        }
        return url;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String value) {
        String result = StringUtils.hasText(value) ? value.trim() : "https://www.tiktok.com";
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
