package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfirmationDetailsResponse {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("html")
    private String html;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}
