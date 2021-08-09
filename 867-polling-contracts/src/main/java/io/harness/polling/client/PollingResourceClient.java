package io.harness.polling.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.CDC)
public interface PollingResourceClient {
  String POLLING_API = "polling";

  @POST(POLLING_API + "/delegate-response/{perpetualTaskId}")
  Call<ResponseDTO> processPolledResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body RequestBody buildSourceExecutionResponse);

  @POST(POLLING_API + "/subscribe") Call<ResponseDTO<byte[]>> subscribe(@Body byte[] pollingItem);

  @POST(POLLING_API + "/unsubscribe") Call<ResponseDTO<Boolean>> unsubscribe(@Body byte[] pollingItem);
}
