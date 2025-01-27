package dev.crashteam.steamauth;

import dev.crashteam.steamauth.helper.Json;
import dev.crashteam.steamauth.model.linker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuthenticatorLinker {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticatorLinker.class);

    private SessionData session;

    public String phoneNumber = null;

    public String phoneCountryCode = null;

    private String deviceID;

    private SteamGuardAccount linkedAccount;

    public boolean finalized = false;

    private boolean confirmationEmailSent = false;

    public String confirmationEmailAddress;

    public AuthenticatorLinker(SessionData sessionData) {
        this.session = sessionData;
        this.deviceID = generateDeviceID();
    }

    public CompletableFuture<LinkResult> addAuthenticator() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (this.confirmationEmailSent) {
                    boolean isStillWaiting = _isAccountWaitingForEmailConfirmation().join();
                    if (isStillWaiting) {
                        return LinkResult.MustConfirmEmail;
                    } else {
                        _sendPhoneVerificationCode().join();
                        Thread.sleep(2000);
                    }
                }

                Map<String, String> addAuthenticatorBody = new LinkedHashMap<>();
                addAuthenticatorBody.put("steamid", String.valueOf(session.getSteamID()));

                long serverTime = TimeAligner.getSteamTime();
                addAuthenticatorBody.put("authenticator_time", String.valueOf(serverTime));
                addAuthenticatorBody.put("authenticator_type", "1");
                addAuthenticatorBody.put("device_identifier", deviceID);
                addAuthenticatorBody.put("sms_phone_id", "1");

                String url = "https://api.steampowered.com/ITwoFactorService/AddAuthenticator/v1/?access_token="
                        + session.getAccessToken();
                String addAuthenticatorResponseStr = SteamWeb.postRequest(url, null, addAuthenticatorBody);

                AddAuthenticatorResponse addAuthenticatorResponse =
                        Json.getInstance().fromJson(addAuthenticatorResponseStr, AddAuthenticatorResponse.class);

                if (addAuthenticatorResponse == null || addAuthenticatorResponse.response == null) {
                    return LinkResult.GeneralFailure;
                }

                int status = addAuthenticatorResponse.response.getStatus();

                if (status == 2) {
                    if (this.phoneNumber == null) {
                        return LinkResult.MustProvidePhoneNumber;
                    } else {
                        String countryCode = this.phoneCountryCode;
                        if (countryCode == null || countryCode.isEmpty()) {
                            countryCode = getUserCountry().join();
                        }
                        SetAccountPhoneNumberResponse res = _setAccountPhoneNumber(phoneNumber, countryCode).join();
                        if (res != null && res.response != null && res.response.confirmationEmailAddress != null) {
                            this.confirmationEmailAddress = res.response.confirmationEmailAddress;
                            this.confirmationEmailSent = true;
                            return LinkResult.MustConfirmEmail;
                        }
                        return LinkResult.FailureAddingPhone;
                    }
                }

                if (status == 29) {
                    return LinkResult.AuthenticatorPresent;
                }

                if (status != 1) {
                    return LinkResult.GeneralFailure;
                }

                this.linkedAccount = addAuthenticatorResponse.response;
                this.linkedAccount.setDeviceID(deviceID);
                this.linkedAccount.setSession(session);

                return LinkResult.AwaitingFinalization;

            } catch (Exception e) {
                LOG.error("Failed add authenticator", e);
                return LinkResult.GeneralFailure;
            }
        });
    }

    public CompletableFuture<FinalizeResult> finalizeAddAuthenticator(String smsCode) {
        return CompletableFuture.supplyAsync(() -> {
            if (this.linkedAccount == null) {
                return FinalizeResult.GeneralFailure;
            }
            int tries = 0;
            while (tries <= 10) {
                try {
                    Map<String, String> finalizeBody = new LinkedHashMap<>();
                    finalizeBody.put("steamid", String.valueOf(session.getSteamID()));
                    String guardCode = this.linkedAccount.generateSteamGuardCode();

                    finalizeBody.put("authenticator_code", guardCode);
                    finalizeBody.put("authenticator_time", String.valueOf(TimeAligner.getSteamTime()));
                    finalizeBody.put("activation_code", smsCode);
                    finalizeBody.put("validate_sms_code", "1");

                    String url = "https://api.steampowered.com/ITwoFactorService/FinalizeAddAuthenticator/v1/?access_token="
                            + session.getAccessToken();
                    String finalizeResultStr = SteamWeb.postRequest(url, null, finalizeBody);

                    FinalizeAuthenticatorResponse finalizeResponse =
                            Json.getInstance().fromJson(finalizeResultStr, FinalizeAuthenticatorResponse.class);

                    if (finalizeResponse == null || finalizeResponse.response == null) {
                        return FinalizeResult.GeneralFailure;
                    }

                    int status = finalizeResponse.response.status;
                    if (status == 89) {
                        return FinalizeResult.BadSMSCode;
                    }
                    if (status == 88) {
                        if (tries >= 10) {
                            return FinalizeResult.UnableToGenerateCorrectCodes;
                        }
                    }
                    if (!finalizeResponse.response.success) {
                        return FinalizeResult.GeneralFailure;
                    }
                    if (finalizeResponse.response.wantMore) {
                        tries++;
                        continue;
                    }

                    this.linkedAccount.setFullyEnrolled(true);
                    this.finalized = true;
                    return FinalizeResult.Success;

                } catch (Exception e) {
                    LOG.error("Failed finalize adding authenticator", e);
                    return FinalizeResult.GeneralFailure;
                }
            }
            return FinalizeResult.GeneralFailure;
        });
    }

    private CompletableFuture<String> getUserCountry() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> body = new LinkedHashMap<>();
                body.put("steamid", String.valueOf(session.getSteamID()));

                String url = "https://api.steampowered.com/IUserAccountService/GetUserCountry/v1?access_token="
                        + session.getAccessToken();
                String responseStr = SteamWeb.postRequest(url, null, body);

                GetUserCountryResponse response = Json.getInstance().fromJson(responseStr, GetUserCountryResponse.class);
                if (response != null && response.response != null && response.response.country != null) {
                    return response.response.country;
                }
            } catch (Exception e) {
                LOG.error("Failed to get user country", e);
            }
            return "";
        });
    }

    private CompletableFuture<SetAccountPhoneNumberResponse> _setAccountPhoneNumber(String phoneNumber, String countryCode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> body = new LinkedHashMap<>();
                body.put("phone_number", phoneNumber);
                body.put("phone_country_code", countryCode);

                String url = "https://api.steampowered.com/IPhoneService/SetAccountPhoneNumber/v1?access_token="
                        + session.getAccessToken();
                String responseStr = SteamWeb.postRequest(url, null, body);

                return Json.getInstance().fromJson(responseStr, SetAccountPhoneNumberResponse.class);
            } catch (Exception e) {
                LOG.error("Failed to set phone number", e);
                return null;
            }
        });
    }

    private CompletableFuture<Boolean> _isAccountWaitingForEmailConfirmation() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://api.steampowered.com/IPhoneService/IsAccountWaitingForEmailConfirmation/v1?access_token="
                        + session.getAccessToken();
                String responseStr = SteamWeb.postRequest(url, null, null);

                IsAccountWaitingForEmailConfirmationResponse resp =
                        Json.getInstance().fromJson(responseStr, IsAccountWaitingForEmailConfirmationResponse.class);
                if (resp != null && resp.response != null) {
                    return resp.response.awaitingEmailConfirmation;
                }
            } catch (Exception e) {
                LOG.error("Failed check is account waiting for email confirm", e);
            }
            return false;
        });
    }

    private CompletableFuture<Boolean> _sendPhoneVerificationCode() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://api.steampowered.com/IPhoneService/SendPhoneVerificationCode/v1?access_token="
                        + session.getAccessToken();
                SteamWeb.postRequest(url, null, null);
            } catch (Exception e) {
                LOG.error("Failed to send phone verification code", e);
                return false;
            }
            return true;
        });
    }

    public static String generateDeviceID() {
        return "android:" + UUID.randomUUID().toString();
    }

    public SessionData getSession() {
        return session;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public SteamGuardAccount getLinkedAccount() {
        return linkedAccount;
    }
}
