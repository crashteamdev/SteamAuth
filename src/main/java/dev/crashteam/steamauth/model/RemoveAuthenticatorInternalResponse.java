package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoveAuthenticatorInternalResponse {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("revocation_attempts_remaining")
    private int revocationAttemptsRemaining;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRevocationAttemptsRemaining() {
        return revocationAttemptsRemaining;
    }

    public void setRevocationAttemptsRemaining(int revocationAttemptsRemaining) {
        this.revocationAttemptsRemaining = revocationAttemptsRemaining;
    }
}
