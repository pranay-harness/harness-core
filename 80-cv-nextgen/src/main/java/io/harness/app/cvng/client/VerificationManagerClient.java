package io.harness.app.cvng.client;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_DATA_COLLECTION_PATH;

import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

/**
 * Interface containing API's to interact with manager.
 * Created by raghu on 09/17/18.
 */
public interface VerificationManagerClient {
  @GET("account/feature-flag-enabled")
  Call<RestResponse<Boolean>> isFeatureEnabled(
      @Query("featureName") String featureName, @Query("accountId") String accountId);

  @GET("delegates/available-versions-for-verification")
  Call<RestResponse<List<String>>> getListOfPublishedVersions(@Query("accountId") String accountId);

  @POST(CV_DATA_COLLECTION_PATH + "/create-task")
  Call<RestResponse<String>> createDataCollectionTask(
      @Query("accountId") String accountId, @Query("cvConfigId") String cvConfigId);
}
