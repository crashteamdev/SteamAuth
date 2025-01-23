package dev.crashteam.steamauth.model.linker;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.crashteam.steamauth.AuthenticatorLinker;

public class GetUserCountryResponse {
    @JsonProperty("response")
    public GetUserCountryResponseResponse response;
}
