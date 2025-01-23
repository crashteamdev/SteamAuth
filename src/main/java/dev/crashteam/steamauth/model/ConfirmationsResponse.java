package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfirmationsResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("needauth")
    private boolean needAuthentication;

    @JsonProperty("conf")
    private Confirmation[] confirmations;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isNeedAuthentication() {
        return needAuthentication;
    }

    public void setNeedAuthentication(boolean needAuthentication) {
        this.needAuthentication = needAuthentication;
    }

    public Confirmation[] getConfirmations() {
        return confirmations;
    }

    public void setConfirmations(Confirmation[] confirmations) {
        this.confirmations = confirmations;
    }
}
