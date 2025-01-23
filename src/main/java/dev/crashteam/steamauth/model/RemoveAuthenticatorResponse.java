package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoveAuthenticatorResponse {
    @JsonProperty("response")
    private RemoveAuthenticatorInternalResponse response;

    public RemoveAuthenticatorInternalResponse getResponse() {
        return response;
    }

    public void setResponse(RemoveAuthenticatorInternalResponse response) {
        this.response = response;
    }
}
