package io.harness.managerclient;

import io.harness.delegate.beans.ResponseData;
import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Query;
import software.wings.common.CICommonEndpointConstants;

/**
 * Temporary Manager Client for helping CI to send delegate tasks from CI to CD
 */

public interface ManagerCIResource {
  @POST("ci" + CICommonEndpointConstants.CI_SETUP_ENDPOINT) Call<RestResponse<ResponseData>> createEnvTask();

  @POST("ci" + CICommonEndpointConstants.CI_COMMAND_EXECUTION_ENDPOINT)
  Call<RestResponse<ResponseData>> commandExecutionTask(
      @Query("analysisContextId") String contextId, @Query("startDataCollectionMinute") long collectionMinute);

  @DELETE("ci" + CICommonEndpointConstants.CI_CLEANUP_ENDPOINT)
  Call<RestResponse<ResponseData>> deleteEnvTask(
      @Query("analysisContextId") String contextId, @Query("startDataCollectionMinute") long collectionMinute);
}
