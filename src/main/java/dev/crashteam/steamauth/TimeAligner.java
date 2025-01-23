package dev.crashteam.steamauth;

import dev.crashteam.steamauth.helper.CommonHelper;
import dev.crashteam.steamauth.helper.Json;
import dev.crashteam.steamauth.model.TimeQuery;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class TimeAligner {
    private static final ThreadFactory THREAD_FACTORY = r -> CommonHelper.newThread("TimeAligner Thread", true, r);
    private static final ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor(THREAD_FACTORY);

    private static boolean aligned = false;
    private static int timeDifference = 0;

    private TimeAligner() {
    }

    public static long getSteamTime() {
        if (!aligned) {
            alignTime();
        }
        return CommonHelper.getUnixTimeStamp() + timeDifference;
    }

    public static long getSteamTimeAsync() {
        if (!aligned) {
            THREAD_POOL.submit(TimeAligner::alignTime);
        }
        return CommonHelper.getUnixTimeStamp() + timeDifference;
    }

    public static synchronized void alignTime() {
        long currentTime = CommonHelper.getUnixTimeStamp();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(APIEndpoints.TWO_FACTOR_TIME_QUERY);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(new StringEntity("steamid=0", StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                String jsonResponse = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                TimeQuery timeQuery = Json.getInstance().fromJson(jsonResponse, TimeQuery.class);
                timeDifference = (int) (timeQuery.getResponse().getServerTime() - currentTime);
                aligned = true;
            }
        } catch (IOException e) {
            System.err.println("Failed to align time: " + e.getMessage());
        }
    }

}
