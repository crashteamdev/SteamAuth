package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SteamAccessToken {
    @JsonProperty("exp")
    private long exp;

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }
}
