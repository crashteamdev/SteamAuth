package dev.crashteam.steamauth.model.linker;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SetAccountPhoneNumberResponseResponse {
    @JsonProperty("confirmation_email_address")
    public String confirmationEmailAddress;

    @JsonProperty("phone_number_formatted")
    public String phoneNumberFormatted;
}
