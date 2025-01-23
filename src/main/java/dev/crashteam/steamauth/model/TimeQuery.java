package dev.crashteam.steamauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeQuery {
    @JsonProperty("response")
    private TimeQueryResponse response;

    public TimeQueryResponse getResponse() {
        return response;
    }

    public void setResponse(TimeQueryResponse response) {
        this.response = response;
    }
}
