package dev.crashteam.steamauth.model.linker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetUserCountryResponseResponse {
    @JsonProperty("country")
    public String country;
}
