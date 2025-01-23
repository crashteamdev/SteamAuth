package dev.crashteam.steamauth.model.linker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IsAccountWaitingForEmailConfirmationResponseResponse {
    @JsonProperty("awaiting_email_confirmation")
    public boolean awaitingEmailConfirmation;

    @JsonProperty("seconds_to_wait")
    public int secondsToWait;
}
