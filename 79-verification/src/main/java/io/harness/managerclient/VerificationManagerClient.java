package io.harness.managerclient;

import static software.wings.common.VerificationConstants.CHECK_STATE_VALID;
import static software.wings.common.VerificationConstants.LAST_SUCCESSFUL_WORKFLOW_IDS;
import static software.wings.common.VerificationConstants.WORKFLOW_FOR_STATE_EXEC;

import io.harness.beans.PageResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.beans.RestResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.common.VerificationConstants;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

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

  @GET("delegates/available-versions-for-verification")
  Call<RestResponse<List<String>>> getListOfPublishedVersions(@Query("accountId") String accountId);

  @POST("apm" + VerificationConstants.NOTIFY_METRIC_STATE)
  Call<RestResponse<Boolean>> sendNotifyForMetricState(@HeaderMap Map<String, Object> headers,
      @Query("correlationId") String correlationId, @Body MetricDataAnalysisResponse metricAnalysisResponse);

  @POST("log-verification" + VerificationConstants.NOTIFY_LOG_STATE)
  Call<RestResponse<Boolean>> sendNotifyForLogState(@HeaderMap Map<String, Object> headers,
      @Query("correlationId") String correlationId, @Body LogAnalysisResponse logAnalysisResponse);

  @GET("account") Call<RestResponse<PageResponse<Account>>> getAccounts(@Query("offset") String offset);

  @GET("account/feature-flag-enabled")
  Call<RestResponse<Boolean>> isFeatureEnabled(
      @Query("featureName") FeatureName featureName, @Query("accountId") String accountId);

  @GET("apm" + VerificationConstants.COLLECT_24_7_DATA)
  Call<RestResponse<Boolean>> triggerAPMDataCollection(@Query("cvConfigId") String cvConfigId,
      @Query("stateType") StateType stateType, @Query("startTime") long startTime, @Query("endTime") long endTime);
}
