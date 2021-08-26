package io.harness.cvng.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.change.event.ChangeEventDTO;
import io.harness.rest.RestResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.CV)
public interface CVNGClient {
  @POST("change-event/register")
  Call<RestResponse<Boolean>> registerChangeEvent(
      @Body ChangeEventDTO changeEventDTO, @Query("accountId") String accountIdentifier);
}
