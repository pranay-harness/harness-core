package io.harness.delegate.task.citasks.cik8handler;

import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CI)
public class HttpHelper {
    public static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

    public Response call(String url, Map<String, String> params) {
        JSONObject j = new JSONObject(params);

        try {
            RequestBody body = RequestBody.create(APPLICATION_JSON, j.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(100, TimeUnit.SECONDS)
                    .writeTimeout(100, TimeUnit.SECONDS)
                    .readTimeout(100, TimeUnit.SECONDS)
                    .build();
            return client.newCall(request).execute();
        } catch (Exception e) {
            log.error("Error sending post data", e);
        }
        return null;
    }
}
