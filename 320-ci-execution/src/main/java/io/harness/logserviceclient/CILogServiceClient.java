package io.harness.logserviceclient;

import io.harness.common.CICommonEndpointConstants;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface CILogServiceClient {
  @GET(CICommonEndpointConstants.LOG_SERVICE_TOKEN_ENDPOINT)
  Call<String> generateToken(@Query("accountID") String accountId, @Header("X-Harness-Token") String globalToken);
}
