package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenerateAccessTokenForAppResponse {
    @JsonProperty("response")
    private GenerateAccessTokenForAppResponseResponse response;

    public GenerateAccessTokenForAppResponseResponse getResponse() {
        return response;
    }

    public void setResponse(GenerateAccessTokenForAppResponseResponse response) {
        this.response = response;
    }
}
