package com.autoshorts.ai.security;

import com.autoshorts.ai.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Symmetric encryption for sensitive secrets stored at rest (e.g. third-party OAuth tokens).
 *
 * <p>Format of an encrypted value: {@code gcm:v1:<base64(iv[12] || ciphertext+tag)>}.
 * Legacy {@code enc:v1:<base64(plaintext)>} values (reversible Base64 from the previous
 * scaffold implementation) are still decryptable so existing rows keep working; they are
 * re-encrypted with AES-GCM the next time they are written.
 */
@Component
public class TokenCipher {

    private static final Logger log = LoggerFactory.getLogger(TokenCipher.class);

    private static final String GCM_PREFIX = "gcm:v1:";
    private static final String LEGACY_PREFIX = "enc:v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenCipher(SecurityProperties securityProperties) {
        this.key = resolveKey(securityProperties);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return GCM_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt token", ex);
        }
    }

    public String decrypt(String stored) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        if (stored.startsWith(GCM_PREFIX)) {
            return decryptGcm(stored.substring(GCM_PREFIX.length()));
        }
        if (stored.startsWith(LEGACY_PREFIX)) {
            byte[] decoded = Base64.getDecoder().decode(stored.substring(LEGACY_PREFIX.length()));
            return new String(decoded, StandardCharsets.UTF_8);
        }
        // Unknown / plaintext value — return as-is so we never lose data.
        return stored;
    }

    private String decryptGcm(String payload) {
        try {
            byte[] combined = Base64.getDecoder().decode(payload);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt token", ex);
        }
    }

    private SecretKeySpec resolveKey(SecurityProperties securityProperties) {
        String configured = securityProperties.getTokenEncryptionKey();
        if (StringUtils.hasText(configured)) {
            byte[] keyBytes = decodeKey(configured.trim());
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new IllegalStateException(
                    "app.security.token-encryption-key must decode to 16, 24, or 32 bytes (got " + keyBytes.length + ")");
            }
            return new SecretKeySpec(keyBytes, "AES");
        }
        // Dev fallback: derive a 256-bit AES key from the JWT secret so local runs work without
        // extra config. Production MUST set an explicit APP_TOKEN_ENCRYPTION_KEY.
        log.warn("app.security.token-encryption-key is not set; deriving an AES key from the JWT secret. "
            + "Set APP_TOKEN_ENCRYPTION_KEY (base64, 32 bytes) before storing real OAuth tokens in production.");
        byte[] derived = sha256(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(derived, "AES");
    }

    private byte[] decodeKey(String configured) {
        try {
            return Base64.getDecoder().decode(configured);
        } catch (IllegalArgumentException ex) {
            // Not base64 — treat as raw passphrase and hash to 256 bits.
            return sha256(configured.getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
