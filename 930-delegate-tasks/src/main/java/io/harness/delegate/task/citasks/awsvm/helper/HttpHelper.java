package io.harness.delegate.task.citasks.awsvm.helper;

import static io.harness.delegate.task.citasks.awsvm.helper.CIAwsVmConstants.RUNNER_CONNECT_TIMEOUT_SECS;
import static io.harness.delegate.task.citasks.awsvm.helper.CIAwsVmConstants.RUNNER_WRITE_TIMEOUT_SECS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CI)
public class HttpHelper {
  public static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

  public Response post(String url, String body, int timeoutInSecs) throws IOException {
    RequestBody requestBody = RequestBody.create(APPLICATION_JSON, body);
    Request request =
        new Request.Builder().url(url).post(requestBody).addHeader("Content-Type", "application/json").build();

    OkHttpClient client = new OkHttpClient.Builder()
                              .connectTimeout(RUNNER_CONNECT_TIMEOUT_SECS, TimeUnit.SECONDS)
                              .writeTimeout(RUNNER_WRITE_TIMEOUT_SECS, TimeUnit.SECONDS)
                              .readTimeout(timeoutInSecs, TimeUnit.SECONDS)
                              .build();
    return client.newCall(request).execute();
  }
}
