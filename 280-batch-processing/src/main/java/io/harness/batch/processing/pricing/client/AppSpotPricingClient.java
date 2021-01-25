package io.harness.batch.processing.pricing.client;

import static io.harness.network.Http.getOkHttpClientBuilder;

import io.harness.network.Http;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Singleton
public class AppSpotPricingClient {
  private static final String BASE_URL = "https://lepton.appspot.com/";
  private static final String ENDPOINT_URL = "skus?format=skuPage&currency=USD&filter=pd&offset=0&limit=1000";

  @ToString
  public static class Sku {
    public List<Sku> skus;
    public String description;
    public List<String> service_regions;
    public List<String> prices;
  }
  @ToString
  public static class ApiResponse {
    public List<Sku> skus;
  }

  private static Call appSpotPricingCall() {
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(120, TimeUnit.SECONDS)
                                    .readTimeout(120, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(BASE_URL))
                                    .retryOnConnectionFailure(true)
                                    .build();
    Request request = new Request.Builder().url(BASE_URL + ENDPOINT_URL).build();
    return okHttpClient.newCall(request);
  }

  public static String fetchJsonEndpoint() throws IOException {
    try (Response response = appSpotPricingCall().execute();) {
      if (!response.isSuccessful()) {
        log.error("AppSpotPricingClient :{}", response.toString());
      }
      // The response returned from appspot is intentionally bad formatted json :(, like
      // )
      // ]
      // }'
      // {
      // "message": "", ...
      return response.body().string().substring(5).trim();
    }
  }

  public static ApiResponse fetchParsedResponse() throws IOException {
    Gson gson = new Gson();
    return gson.fromJson(fetchJsonEndpoint(), ApiResponse.class);
  }
}
