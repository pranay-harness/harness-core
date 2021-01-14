package io.harness.nexus;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.WingsException;
import io.harness.network.Http;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

@UtilityClass
@Slf4j
public class NexusHelper {
  static String getBaseUrl(NexusRequest nexusRequest) {
    return nexusRequest.getNexusUrl().endsWith("/") ? nexusRequest.getNexusUrl() : nexusRequest.getNexusUrl() + "/";
  }

  static Retrofit getRetrofit(NexusRequest nexusRequest, Converter.Factory converterFactory) {
    String baseUrl = getBaseUrl(nexusRequest);
    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converterFactory)
        .client(Http.getOkHttpClient(baseUrl, nexusRequest.isCertValidationRequired()))
        .build();
  }

  public static boolean isSuccessful(Response<?> response) {
    if (response == null) {
      return false;
    }
    if (!response.isSuccessful()) {
      log.error("Request not successful. Reason: {}", response);
      int code = response.code();
      switch (code) {
        case 404:
          return false;
        case 401:
          throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Invalid Nexus credentials");
        case 405:
          throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
              .addParam("message", "Method not allowed" + response.message());
        default:
          throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", response.message());
      }
    }
    return true;
  }
}
