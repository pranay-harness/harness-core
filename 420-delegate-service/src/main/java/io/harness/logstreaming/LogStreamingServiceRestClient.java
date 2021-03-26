package io.harness.logstreaming;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

@TargetModule(HarnessModule._980_COMMONS)
public interface LogStreamingServiceRestClient {
  @GET("token")
  Call<String> retrieveAccountToken(
      @Header("X-Harness-Token") String serviceToken, @Query("accountID") String accountId);
}
