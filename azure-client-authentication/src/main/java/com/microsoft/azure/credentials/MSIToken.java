package com.microsoft.azure.credentials;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Type representing response from the local MSI token provider.
 */
class MSIToken {
    /**
     * Token type "Bearer".
     */
    @JsonProperty(value = "token_type")
    private String tokenType;

    /**
     * Access token.
     */
    @JsonProperty(value = "access_token")
    private String accessToken;

    String accessToken() {
        return accessToken;
    }

    String tokenType() {
        return tokenType;
    }
}