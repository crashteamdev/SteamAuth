package dev.crashteam.steamauth;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SteamWeb {
    public static final String MOBILE_APP_USER_AGENT = "Dalvik/2.1.0 (Linux; U; Android 9; Valve Steam App Version/3)";

    public static String getRequest(String url, Map<String, String> cookies) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", MOBILE_APP_USER_AGENT);

            if (cookies != null && !cookies.isEmpty()) {
                String cookieHeader = cookies.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce((c1, c2) -> c1 + "; " + c2)
                        .orElse("");
                httpGet.setHeader("Cookie", cookieHeader);
            }

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    public static String postRequest(String url, Map<String, String> cookies, Map<String, String> body) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("User-Agent", MOBILE_APP_USER_AGENT);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            if (cookies != null && !cookies.isEmpty()) {
                String cookieHeader = cookies.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce((c1, c2) -> c1 + "; " + c2)
                        .orElse("");
                httpPost.setHeader("Cookie", cookieHeader);
            }

            if (body != null && !body.isEmpty()) {
                List<NameValuePair> form = new ArrayList<>();
                for (Map.Entry<String, String> entry : body.entrySet()) {
                    form.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));
            }

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }
}
