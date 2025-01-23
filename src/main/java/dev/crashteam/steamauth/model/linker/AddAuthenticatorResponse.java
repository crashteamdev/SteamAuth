package dev.crashteam.steamauth.model.linker;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.crashteam.steamauth.SteamGuardAccount;

public class AddAuthenticatorResponse {
    @JsonProperty("response")
    public SteamGuardAccount response;
}
