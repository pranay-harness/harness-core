package io.harness.managerclient;

import static software.wings.common.VerificationConstants.CHECK_STATE_VALID;
import static software.wings.common.VerificationConstants.LAST_SUCCESSFUL_WORKFLOW_IDS;
import static software.wings.common.VerificationConstants.WORKFLOW_FOR_STATE_EXEC;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import software.wings.beans.RestResponse;
import software.wings.beans.WorkflowExecution;

import java.util.List;

/**
 * Interface containing API's to interact with manager.
 * Created by raghu on 09/17/18.
 */
public interface VerificationManagerClient {
  @GET("workflows" + LAST_SUCCESSFUL_WORKFLOW_IDS)
  Call<RestResponse<List<String>>> getLastSuccessfulWorkflowExecutionIds(
      @Query("appId") String appId, @Query("workflowId") String workflowId, @Query("serviceId") String serviceId);

  @GET("workflows" + CHECK_STATE_VALID)
  Call<RestResponse<Boolean>> isStateValid(
      @Query("appId") String appId, @Query("stateExecutionId") String stateExecutionId);

  @GET("workflows" + WORKFLOW_FOR_STATE_EXEC)
  Call<RestResponse<WorkflowExecution>> getWorkflowExecution(
      @Query("appId") String appId, @Query("stateExecutionId") String stateExecutionId);
}
