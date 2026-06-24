package com.autoshorts.ai.dto;

/**
 * Response carrying the TikTok authorize URL the frontend should redirect the user to.
 */
public record TikTokAuthorizationResponse(String authorizationUrl) {
}
