package io.harness.managerclient;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.rest.RestResponse;
import io.harness.serializer.kryo.KryoResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.beans.ConfigFile;
import software.wings.beans.Delegate;
import software.wings.beans.Log;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.manifest.ManifestCollectionExecutionResponse;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.util.List;

public interface ManagerClient {
  @GET("service-templates/{templateId}/compute-files")
  Call<RestResponse<List<ConfigFile>>> getConfigFiles(@Path("templateId") String templateId,
      @Query("accountId") String accountId, @Query("appId") String appId, @Query("envId") String envId,
      @Query("hostId") String hostId);

  @POST("logs/activity/{activityId}/unit/{unitName}/batched")
  Call<RestResponse> saveCommandUnitLogs(@Path("activityId") String activityId, @Path("unitName") String unitName,
      @Query("accountId") String accountId, @Body Log logObject);

  @POST("agent/delegates/{delegateId}/state-executions")
  Call<RestResponse> saveApiCallLogs(@Path("delegateId") String delegateId, @Query("accountId") String accountId,
      @Body List<ThirdPartyApiCallLog> logObject);

  @POST(LogAnalysisResource.LOG_ANALYSIS + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  Call<RestResponse<Boolean>> saveLogs(@Query("accountId") String accountId, @Query("appId") String appId,
      @Query("stateExecutionId") String stateExecutionId, @Query("workflowId") String workflowId,
      @Query("workflowExecutionId") String workflowExecutionId, @Query("serviceId") String serviceId,
      @Query("clusterLevel") ClusterLevel clusterLevel, @Query("delegateTaskId") String delegateTaskId,
      @Query("stateType") StateType stateType, @Body List<LogElement> metricData);

  @KryoResponse
  @PUT("agent/delegates/{delegateId}/tasks/{taskId}/acquire")
  Call<DelegateTaskPackage> acquireTask(
      @Path("delegateId") String delegateId, @Path("taskId") String uuid, @Query("accountId") String accountId);

  @KryoResponse
  @POST("agent/delegates/{delegateId}/tasks/{taskId}/report")
  Call<DelegateTaskPackage> reportConnectionResults(@Path("delegateId") String delegateId, @Path("taskId") String uuid,
      @Query("accountId") String accountId, @Body List<DelegateConnectionResult> results);

  @POST("agent/delegates/heartbeat-with-polling")
  Call<RestResponse<Delegate>> delegateHeartbeat(@Query("accountId") String accountId, @Body Delegate delegate);

  @POST("agent/delegates/artifact-collection/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishArtifactCollectionResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body BuildSourceExecutionResponse buildSourceExecutionResponse);

  @POST("agent/delegates/manifest-collection/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishManifestCollectionResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId,
      @Body ManifestCollectionExecutionResponse manifestCollectionExecutionResponse);
}
