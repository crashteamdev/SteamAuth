package dev.crashteam.steamauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.crashteam.steamauth.crypto.HMACSHA1;
import dev.crashteam.steamauth.helper.IOHelper;
import dev.crashteam.steamauth.helper.Json;
import dev.crashteam.steamauth.model.Confirmation;
import dev.crashteam.steamauth.model.ConfirmationsResponse;
import dev.crashteam.steamauth.model.RemoveAuthenticatorResponse;
import dev.crashteam.steamauth.model.SendConfirmationResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SteamGuardAccount {

    private final Logger LOG = LoggerFactory.getLogger(SteamGuardAccount.class);

    @JsonProperty("shared_secret")
    private String sharedSecret;

    @JsonProperty("serial_number")
    private String serialNumber;

    @JsonProperty("revocation_code")
    private String revocationCode;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("server_time")
    private long serverTime;

    @JsonProperty("account_name")
    private String accountName;

    @JsonProperty("token_gid")
    private String tokenGID;

    @JsonProperty("identity_secret")
    private String identitySecret;

    @JsonProperty("secret_1")
    private String secret1;

    @JsonProperty("status")
    private int status;

    @JsonProperty("device_id")
    private String deviceID;

    @JsonProperty("fully_enrolled")
    private boolean fullyEnrolled;

    private SessionData session;

    private static final byte[] STEAM_GUARD_CODE_TRANSLATIONS = {
            50, 51, 52, 53, 54, 55, 56, 57,
            66, 67, 68, 70, 71, 72, 74, 75,
            77, 78, 80, 81, 82, 84, 86, 87,
            88, 89
    };

    public CompletableFuture<Boolean> deactivateAuthenticator(int scheme) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                LinkedHashMap<String, String> postBody = new LinkedHashMap<>();
                postBody.put("revocation_code", this.revocationCode);
                postBody.put("revocation_reason", "1");
                postBody.put("steamguard_scheme", String.valueOf(scheme));

                String url = "https://api.steampowered.com/ITwoFactorService/RemoveAuthenticator/v1?access_token="
                        + this.session.getAccessToken();

                String response = SteamWeb.postRequest(url, null, postBody);
                RemoveAuthenticatorResponse removeResponse =
                        Json.getInstance().fromJson(response, RemoveAuthenticatorResponse.class);

                return removeResponse != null
                        && removeResponse.getResponse() != null
                        && removeResponse.getResponse().isSuccess();

            } catch (Exception e) {
                LOG.error("Deactivate authenticator error", e);
                return false;
            }
        });
    }

    public String generateSteamGuardCode() {
        long currentTime = TimeAligner.getSteamTime();
        return generateSteamGuardCodeForTime(currentTime);
    }

    public java.util.concurrent.CompletableFuture<String> generateSteamGuardCodeAsync() {
        long steamTimeAsync = TimeAligner.getSteamTimeAsync();
        return CompletableFuture.supplyAsync(() -> generateSteamGuardCodeForTime(steamTimeAsync));
    }

    public String generateSteamGuardCodeForTime(long time) {
        if (this.sharedSecret == null || this.sharedSecret.isEmpty()) {
            return "";
        }

        // Shared secret is our key
        byte[] sharedSecretArray = Base64.getDecoder().decode(sharedSecret);

        // Time for code
        time /= 30L;
        byte[] timeArray = new byte[8];
        long temp = time;
        for (int i = 8; i > 0; i--) {
            timeArray[i - 1] = (byte) (temp & 0xFF);
            temp >>= 8;
        }

        // Generate hmac
        byte[] codeArray = new byte[5];
        try {
            byte[] hashedData = HMACSHA1.calculate(timeArray, sharedSecretArray);

            // the last 4 bits of the hashedData say where the code starts
            // (e.g. if last 4 bit are 1100, we start at byte 12)
            byte b = (byte) (hashedData[19] & 0xF);

            int codePoint = (hashedData[b] & 0x7F) << 24 | (hashedData[b + 1] & 0xFF) << 16 | (hashedData[b + 2] & 0xFF) << 8 | (hashedData[b + 3] & 0xFF);

            for (int i = 0; i < 5; ++i) {
                codeArray[i] = STEAM_GUARD_CODE_TRANSLATIONS[codePoint % STEAM_GUARD_CODE_TRANSLATIONS.length];
                codePoint /= STEAM_GUARD_CODE_TRANSLATIONS.length;
            }
        } catch (Exception e) {
            LOG.error("Failed to generate hmac", e);
            return null;
        }
        return IOHelper.decode(codeArray);
    }

    public Confirmation[] fetchConfirmations() throws Exception {
        String url = generateConfirmationURL("conf");
        Map<String, String> cookieMap = new LinkedHashMap<>();
        for (Cookie c : session.getCookies().getCookies()) {
            cookieMap.put(c.getName(), c.getValue());
        }
        String response = SteamWeb.getRequest(url, cookieMap);
        return fetchConfirmationInternal(response);
    }

    public java.util.concurrent.CompletableFuture<Confirmation[]> fetchConfirmationsAsync() throws Exception {
        String url = generateConfirmationURL("conf");
        Map<String, String> cookieMap = new LinkedHashMap<>();
        for (Cookie c : session.getCookies().getCookies()) {
            cookieMap.put(c.getName(), c.getValue());
        }
        String response = SteamWeb.getRequest(url, cookieMap);

        return CompletableFuture.supplyAsync(() -> fetchConfirmationInternal(response));
    }

    private Confirmation[] fetchConfirmationInternal(String response) {
        ConfirmationsResponse confirmationsResponse;
        confirmationsResponse = Json.getInstance().fromJson(response, ConfirmationsResponse.class);

        if (!confirmationsResponse.isSuccess()) {
            throw new RuntimeException(confirmationsResponse.getMessage());
        }
        if (confirmationsResponse.isNeedAuthentication()) {
            throw new RuntimeException("Needs Authentication");
        }

        return confirmationsResponse.getConfirmations();
    }

    /**
     * В C#: public long GetConfirmationTradeOfferID(Confirmation conf)
     */
    public long getConfirmationTradeOfferID(Confirmation conf) {
        if (conf.getConfType() != Confirmation.EMobileConfirmationType.Trade) {
            throw new IllegalArgumentException("conf must be a trade confirmation.");
        }
        return conf.getCreator();
    }

    // Методы Accept / Deny для одиночных и множественных подтверждений
    public CompletableFuture<Boolean> acceptMultipleConfirmations(Confirmation[] confs) {
        return _sendMultiConfirmationAjax(confs, "allow");
    }

    public CompletableFuture<Boolean> denyMultipleConfirmations(Confirmation[] confs) {
        return _sendMultiConfirmationAjax(confs, "cancel");
    }

    public CompletableFuture<Boolean> acceptConfirmation(Confirmation conf) {
        return _sendConfirmationAjax(conf, "allow");
    }

    public CompletableFuture<Boolean> denyConfirmation(Confirmation conf) {
        return _sendConfirmationAjax(conf, "cancel");
    }

    private CompletableFuture<Boolean> _sendConfirmationAjax(Confirmation conf, String op) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                String url = APIEndpoints.COMMUNITY_BASE + "/mobileconf/ajaxop";
                String queryString = "?op=" + op + "&";

                // tag отличается от op
                String tag = "allow".equals(op) ? "accept" : "reject";
                queryString += generateConfirmationQueryParams(tag);
                queryString += "&cid=" + conf.getId() + "&ck=" + conf.getKey();

                url += queryString;

                Map<String, String> cookieMap = new LinkedHashMap<>();
                for (Cookie c : session.getCookies().getCookies()) {
                    cookieMap.put(c.getName(), c.getValue());
                }
                String response = SteamWeb.getRequest(url, cookieMap);
                if (response == null) return false;

                SendConfirmationResponse confResponse =
                        Json.getInstance().fromJson(response, SendConfirmationResponse.class);

                return confResponse.isSuccess();
            } catch (Exception e) {
                LOG.error("Failed to send ajax confirmation", e);
                return false;
            }
        });
    }

    private CompletableFuture<Boolean> _sendMultiConfirmationAjax(Confirmation[] confs, String op) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultCookieStore(session.getCookies())
                    .build()) {

                String url = APIEndpoints.COMMUNITY_BASE + "/mobileconf/multiajaxop";
                String tag = "allow".equals(op) ? "accept" : "reject";

                StringBuilder query = new StringBuilder();
                query.append("op=").append(op)
                        .append("&")
                        .append(generateConfirmationQueryParams(tag));

                for (Confirmation conf : confs) {
                    query.append("&cid[]=").append(conf.getId())
                            .append("&ck[]=").append(conf.getKey());
                }

                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("User-Agent", SteamWeb.MOBILE_APP_USER_AGENT);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                StringEntity entity = new StringEntity(query.toString(), StandardCharsets.UTF_8);
                httpPost.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String responseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                    if (responseString == null) {
                        return false;
                    }

                    SendConfirmationResponse confResponse =
                            Json.getInstance().fromJson(responseString, SendConfirmationResponse.class);

                    return confResponse.isSuccess();
                }
            } catch (Exception e) {
                LOG.error("Failed to send multi ajax confirmation", e);
                return false;
            }
        });
    }

    public String generateConfirmationURL(String tag) {
        String endpoint = APIEndpoints.COMMUNITY_BASE + "/mobileconf/getlist?";
        String queryString = generateConfirmationQueryParams(tag);
        return endpoint + queryString;
    }

    public String generateConfirmationQueryParams(String tag) {
        Map<String, String> nvc = generateConfirmationQueryParamsAsNVC(tag);

        StringBuilder sb = new StringBuilder();
        for (String key : nvc.keySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(key).append("=").append(nvc.get(key));
        }

        return sb.toString();
    }

    public Map<String, String> generateConfirmationQueryParamsAsNVC(String tag) {
        if (this.deviceID == null || this.deviceID.isEmpty()) {
            throw new IllegalArgumentException("Device ID is not present");
        }

        long time = TimeAligner.getSteamTime();

        Map<String, String> ret = new LinkedHashMap<>();
        ret.put("p", this.deviceID);
        ret.put("a", String.valueOf(this.session.getSteamID()));
        ret.put("k", _generateConfirmationHashForTime(time, tag));
        ret.put("t", String.valueOf(time));
        ret.put("m", "react");
        ret.put("tag", tag);

        return ret;
    }

    private String _generateConfirmationHashForTime(long time, String tag) {
        byte[] decode = Base64.getDecoder().decode(identitySecret);
        int n2 = 8;
        if (tag != null) {
            if (tag.length() > 32) {
                n2 = 8 + 32;
            } else {
                n2 = 8 + tag.length();
            }
        }
        byte[] array = new byte[n2];
        int n3 = 8;
        while (true) {
            int n4 = n3 - 1;
            if (n3 <= 0) {
                break;
            }
            array[n4] = (byte) time;
            time >>= 8;
            n3 = n4;
        }
        if (tag != null) {
            System.arraycopy(IOHelper.encode(tag), 0, array, 8, n2 - 8);
        }

        try {
            byte[] hashedData = HMACSHA1.calculate(array, decode);
            String encodedData = Base64.getEncoder().encodeToString(hashedData);

            return URLEncoder.encode(encodedData, StandardCharsets.UTF_8);
        } catch (InvalidKeyException e) {
            LOG.error("Failed to generate confirmation hash", e);
            return null;
        }
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getRevocationCode() {
        return revocationCode;
    }

    public void setRevocationCode(String revocationCode) {
        this.revocationCode = revocationCode;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getTokenGID() {
        return tokenGID;
    }

    public void setTokenGID(String tokenGID) {
        this.tokenGID = tokenGID;
    }

    public String getIdentitySecret() {
        return identitySecret;
    }

    public void setIdentitySecret(String identitySecret) {
        this.identitySecret = identitySecret;
    }

    public String getSecret1() {
        return secret1;
    }

    public void setSecret1(String secret1) {
        this.secret1 = secret1;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public boolean isFullyEnrolled() {
        return fullyEnrolled;
    }

    public void setFullyEnrolled(boolean fullyEnrolled) {
        this.fullyEnrolled = fullyEnrolled;
    }

    public SessionData getSession() {
        return session;
    }

    public void setSession(SessionData session) {
        this.session = session;
    }
}
