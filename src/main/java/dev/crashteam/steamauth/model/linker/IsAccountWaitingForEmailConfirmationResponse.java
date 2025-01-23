package dev.crashteam.steamauth.model.linker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IsAccountWaitingForEmailConfirmationResponse {
    @JsonProperty("response")
    public IsAccountWaitingForEmailConfirmationResponseResponse response;
}
