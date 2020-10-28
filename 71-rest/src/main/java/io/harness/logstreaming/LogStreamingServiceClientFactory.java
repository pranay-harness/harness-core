package io.harness.logstreaming;

import static io.harness.network.Http.getOkHttpClientBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;

import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import software.wings.app.MainConfiguration;

import java.util.concurrent.TimeUnit;

@Slf4j
public class LogStreamingServiceClientFactory implements Provider<LogStreamingServiceRestClient> {
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public LogStreamingServiceRestClient get() {
    String url = mainConfiguration.getLogStreamingServiceConfig().getBaseUrl();
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(5, TimeUnit.SECONDS)
                                    .readTimeout(10, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .retryOnConnectionFailure(true)
                                    .build();

    Gson gson = new GsonBuilder().setLenient().create();

    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();

    return retrofit.create(LogStreamingServiceRestClient.class);
  }
}
