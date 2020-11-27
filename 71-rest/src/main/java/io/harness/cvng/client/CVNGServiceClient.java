package io.harness.cvng.client;

import static io.harness.cvng.core.services.CVNextGenConstants.INTERNAL_ACTIVITY_RESOURCE;

import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.rest.RestResponse;

import javax.validation.Valid;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface CVNGServiceClient {
  @POST(INTERNAL_ACTIVITY_RESOURCE)
  Call<RestResponse<String>> registerActivity(
      @Query("accountId") @Valid String accountId, @Body ActivityDTO activityDTO);
  @GET(INTERNAL_ACTIVITY_RESOURCE + "/activity-status")
  Call<RestResponse<ActivityStatusDTO>> getActivityStatus(
      @Query("accountId") String accountId, @Query("activityId") String activityId);
}
