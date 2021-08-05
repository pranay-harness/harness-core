package io.harness.poll;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.CDC)
public interface PollResourceClient {
  String POLLING_API = "poll";

  @POST(POLLING_API + "/delegate-response/{perpetualTaskId}")
  Call<ResponseDTO> processPolledResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body byte[] buildSourceExecutionResponse);
}
