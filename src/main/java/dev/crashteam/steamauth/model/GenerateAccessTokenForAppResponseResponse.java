package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenerateAccessTokenForAppResponseResponse {
    @JsonProperty("access_token")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
