package io.harness.logserviceclient;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.exception.GeneralException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;

@Getter
@Setter
@Slf4j
@Singleton
public class CILogServiceUtils {
  private final CILogServiceClient ciLogServiceClient;
  private final LogServiceConfig logServiceConfig;

  @Inject
  public CILogServiceUtils(CILogServiceClient logServiceClient, LogServiceConfig logServiceConfig) {
    this.ciLogServiceClient = logServiceClient;
    this.logServiceConfig = logServiceConfig;
  }

  @NotNull
  public String getLogServiceToken(String accountID) throws Exception {
    log.info("Initiating token request to log service: " + this.logServiceConfig.getBaseUrl());
    Call<String> tokenCall = ciLogServiceClient.generateToken(accountID, this.logServiceConfig.getGlobalToken());
    Response<String> response = tokenCall.execute();
    // Received error from the server
    if (!response.isSuccessful()) {
      throw new GeneralException(
          String.format("Could not fetch token from log service. status code = %s, message = %s, response = %s",
              response.code(), response.message() == null ? "null" : response.message(),
              response.errorBody() == null ? "null" : response.errorBody().string()));
    }
    return response.body();
  }
}
