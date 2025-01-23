package dev.crashteam.steamauth.model.linker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FinalizeAuthenticatorInternalResponse {
    @JsonProperty("success")
    public boolean success;

    @JsonProperty("want_more")
    public boolean wantMore;

    @JsonProperty("server_time")
    public long serverTime;

    @JsonProperty("status")
    public int status;
}
