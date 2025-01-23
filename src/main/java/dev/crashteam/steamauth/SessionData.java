package dev.crashteam.steamauth;

import dev.crashteam.steamauth.helper.Json;
import dev.crashteam.steamauth.model.GenerateAccessTokenForAppResponse;
import dev.crashteam.steamauth.model.SteamAccessToken;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import java.time.Instant;
import java.util.*;

public class SessionData {

    private long steamID;
    private String accessToken;
    private String refreshToken;
    private String sessionID;

    public long getSteamID() {
        return steamID;
    }

    public void setSteamID(long steamID) {
        this.steamID = steamID;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public void refreshAccessToken() throws Exception {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new Exception("Refresh token is empty");
        }

        if (isTokenExpired(refreshToken)) {
            throw new Exception("Refresh token is expired");
        }

        Map<String, String> postData = new HashMap<>();
        postData.put("refresh_token", refreshToken);
        postData.put("steamid", String.valueOf(steamID));

        String responseStr = SteamWeb.postRequest(
                "https://api.steampowered.com/IAuthenticationService/GenerateAccessTokenForApp/v1/",
                null, postData);

        GenerateAccessTokenForAppResponse response = Json.getInstance().fromJson(responseStr, GenerateAccessTokenForAppResponse.class);
        this.accessToken = response.getResponse().getAccessToken();
    }

    public boolean isAccessTokenExpired() {
        return accessToken == null || isTokenExpired(accessToken);
    }

    public boolean isRefreshTokenExpired() {
        return refreshToken == null || isTokenExpired(refreshToken);
    }

    private boolean isTokenExpired(String token) {
        String[] tokenComponents = token.split("\\.");
        StringBuilder base64 = new StringBuilder(
                tokenComponents[1].replace('-', '+').replace('_', '/'));

        while (base64.length() % 4 != 0) {
            base64.append("=");
        }

        try {
            byte[] payloadBytes = Base64.getDecoder().decode(base64.toString());
            String payload = new String(payloadBytes);
            SteamAccessToken jwt = Json.getInstance().fromJson(payload, SteamAccessToken.class);

            return Instant.now().getEpochSecond() > jwt.getExp();
        } catch (Exception e) {
            return true;
        }
    }

    public CookieStore getCookies() {
        if (this.sessionID == null) {
            this.sessionID = generateSessionID();
        }
        CookieStore cookieStore = new BasicCookieStore();

        for (String domain : new String[]{"steamcommunity.com", "store.steampowered.com"}) {
            BasicClientCookie steamLoginSecure = new BasicClientCookie("steamLoginSecure", getSteamLoginSecure());
            steamLoginSecure.setDomain(domain);
            steamLoginSecure.setPath("/");

            BasicClientCookie sessionid = new BasicClientCookie("sessionid", this.sessionID);
            sessionid.setDomain(domain);
            sessionid.setPath("/");

            BasicClientCookie mobileClient = new BasicClientCookie("mobileClient", "android");
            mobileClient.setDomain(domain);
            mobileClient.setPath("/");

            BasicClientCookie mobileClientVersion = new BasicClientCookie("mobileClientVersion", "777777 3.6.4");
            mobileClientVersion.setDomain(domain);
            mobileClientVersion.setPath("/");

            cookieStore.addCookie(steamLoginSecure);
            cookieStore.addCookie(sessionid);
            cookieStore.addCookie(mobileClient);
            cookieStore.addCookie(mobileClientVersion);
        }

        return cookieStore;
    }

    private String getSteamLoginSecure() {
        return steamID + "%7C%7C" + accessToken;
    }

    private static String generateSessionID() {
        return getRandomHexNumber(32);
    }

    private static String getRandomHexNumber(int digits) {
        if (digits < 1) {
            throw new IllegalArgumentException("Number of digits must be >= 1");
        }
        Random random = new Random();
        byte[] buffer = new byte[digits / 2];
        random.nextBytes(buffer);

        StringBuilder sb = new StringBuilder(digits);
        for (byte b : buffer) {
            sb.append(String.format("%02X", b));
        }
        if (digits % 2 != 0) {
            sb.append(String.format("%X", random.nextInt(16)));
        }

        return sb.toString();
    }
}
