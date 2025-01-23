package dev.crashteam.steamauth.model.linker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FinalizeAuthenticatorResponse {
    @JsonProperty("response")
    public FinalizeAuthenticatorInternalResponse response;
}
