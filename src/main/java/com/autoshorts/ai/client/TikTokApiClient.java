package com.autoshorts.ai.client;

import com.autoshorts.ai.client.model.TikTokCreatorInfo;
import com.autoshorts.ai.client.model.TikTokPublishInit;
import com.autoshorts.ai.client.model.TikTokPublishStatus;
import com.autoshorts.ai.client.model.TikTokTokenResponse;

/**
 * Thin wrapper over the TikTok OAuth and Content Posting APIs.
 *
 * <p>All methods are synchronous and throw
 * {@link com.autoshorts.ai.exception.ExternalServiceException} on transport or API errors.
 */
public interface TikTokApiClient {

    /** Exchange an authorization code from the OAuth redirect for access/refresh tokens. */
    TikTokTokenResponse exchangeAuthorizationCode(String code);

    /** Obtain a fresh access token from a stored refresh token. */
    TikTokTokenResponse refreshAccessToken(String refreshToken);

    /** Query creator info — required by TikTok before initializing a direct post. */
    TikTokCreatorInfo queryCreatorInfo(String accessToken);

    /**
     * Initialize a direct post that pulls the video from a publicly reachable URL.
     *
     * @param videoUrl     publicly accessible HTTPS URL of the rendered video
     * @param caption      post caption / title
     * @param privacyLevel one of the creator's allowed privacy levels
     */
    TikTokPublishInit initDirectPostFromUrl(String accessToken, String videoUrl, String caption, String privacyLevel);

    /** Poll the processing status of a previously initialized post. */
    TikTokPublishStatus fetchPostStatus(String accessToken, String publishId);
}
