package io.harness.ng;

import static io.harness.network.Http.getOkHttpClientBuilder;

import io.harness.logstreaming.LogStreamingServiceRestClient;
import io.harness.network.Http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NGLogStreamingClientFactory implements Provider<LogStreamingServiceRestClient> {
  @Inject private NextGenConfiguration mainConfiguration;

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
