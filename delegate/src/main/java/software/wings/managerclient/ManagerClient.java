package software.wings.managerclient;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.beans.ConfigFile;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;

import java.util.List;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public interface ManagerClient {
  @POST("delegates/register")
  Call<RestResponse<Delegate>> registerDelegate(@Query("accountId") String accountId, @Body Delegate delegate);

  @Headers({"Content-Type: application/x-kryo"})
  @KryoRequest
  @POST("delegates/{delegateId}/tasks/{taskId}")
  Call<ResponseBody> sendTaskStatus(@Path("delegateId") String delegateId, @Path("taskId") String taskId,
      @Query("accountId") String accountId, @Body DelegateTaskResponse delegateTaskResponse);

  @Multipart
  @POST("delegateFiles/{delegateId}/tasks/{taskId}")
  Call<RestResponse<String>> uploadFile(@Path("delegateId") String delegateId, @Path("taskId") String taskId,
      @Query("accountId") String accountId, @Part MultipartBody.Part file);

  @GET("delegates/{delegateId}/upgrade")
  Call<RestResponse<DelegateScripts>> checkForUpgrade(
      @Header("Version") String version, @Path("delegateId") String delegateId, @Query("accountId") String accountId);

  @GET("service-templates/{templateId}/compute-files")
  Call<RestResponse<List<ConfigFile>>> getConfigFiles(@Path("templateId") String templateId,
      @Query("accountId") String accountId, @Query("appId") String appId, @Query("envId") String envId,
      @Query("hostId") String hostId);

  @POST("logs/batched")
  Call<RestResponse<List<String>>> batchedSaveLogs(@Query("accountId") String accountId, @Body List<Log> logs);

  @POST("logs/activity/{activityId}/unit/{unitName}/batched")
  Call<RestResponse> saveCommandUnitLogs(@Path("activityId") String activityId, @Path("unitName") String unitName,
      @Query("accountId") String accountId, @Body Log log);

  @POST("appdynamics/save-metrics")
  Call<RestResponse<Boolean>> saveAppdynamicsMetrics(@Query("accountId") String accountId,
      @Query("applicationId") String applicationId, @Query("stateExecutionId") String stateExecutionId,
      @Query("appdynamicsAppId") long appId, @Query("tierId") long tierId,
      @Body List<AppdynamicsMetricData> metricData);

  @POST("newrelic/save-metrics")
  Call<RestResponse<Boolean>> saveNewRelicMetrics(@Query("accountId") String accountId,
      @Query("applicationId") String applicationId, @Query("stateExecutionId") String stateExecutionId,
      @Query("delegateTaskId") String delegateTaskId, @Body List<NewRelicMetricDataRecord> metricData);

  @POST("newrelic/save-metric-names")
  Call<RestResponse<Boolean>> saveNewRelicMetricNames(
      @Query("accountId") String accountId, @Body NewRelicMetricNames metricData);

  @POST("newrelic/get-metric-names")
  Call<RestResponse<NewRelicMetricNames>> getNewRelicMetricNames(
      @Query("accountId") String accountId, @Body NewRelicMetricNames metricNames);

  @POST(LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  Call<RestResponse<Boolean>> saveSplunkLogs(@Query("accountId") String accountId, @Query("appId") String appId,
      @Query("stateExecutionId") String stateExecutionId, @Query("workflowId") String workflowId,
      @Query("workflowExecutionId") String workflowExecutionId, @Query("serviceId") String serviceId,
      @Query("clusterLevel") ClusterLevel clusterLevel, @Query("delegateTaskId") String delegateTaskId,
      @Body List<LogElement> metricData);

  @POST(LogAnalysisResource.ELK_RESOURCE_BASE_URL + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  Call<RestResponse<Boolean>> saveElkLogs(@Query("accountId") String accountId, @Query("appId") String appId,
      @Query("stateExecutionId") String stateExecutionId, @Query("workflowId") String workflowId,
      @Query("workflowExecutionId") String workflowExecutionId, @Query("serviceId") String serviceId,
      @Query("clusterLevel") ClusterLevel clusterLevel, @Query("delegateTaskId") String delegateTaskId,
      @Body List<LogElement> metricData);

  @POST(LogAnalysisResource.LOGZ_RESOURCE_BASE_URL + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  Call<RestResponse<Boolean>> saveLogzLogs(@Query("accountId") String accountId, @Query("appId") String appId,
      @Query("stateExecutionId") String stateExecutionId, @Query("workflowId") String workflowId,
      @Query("workflowExecutionId") String workflowExecutionId, @Query("serviceId") String serviceId,
      @Query("clusterLevel") ClusterLevel clusterLevel, @Query("delegateTaskId") String delegateTaskId,
      @Body List<LogElement> metricData);

  @POST(LogAnalysisResource.SUMO_RESOURCE_BASE_URL + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  Call<RestResponse<Boolean>> saveSumoLogs(@Query("accountId") String accountId, @Query("appId") String appId,
      @Query("stateExecutionId") String stateExecutionId, @Query("workflowId") String workflowId,
      @Query("workflowExecutionId") String workflowExecutionId, @Query("serviceId") String serviceId,
      @Query("clusterLevel") ClusterLevel clusterLevel, @Query("delegateTaskId") String delegateTaskId,
      @Body List<LogElement> metricData);

  @GET("delegateFiles/fileId")
  Call<RestResponse<String>> getFileIdByVersion(@Query("entityId") String entityId,
      @Query("fileBucket") FileBucket fileBucket, @Query("version") int version, @Query("accountId") String accountId);

  @GET("delegateFiles/download")
  Call<ResponseBody> downloadFile(@Query("fileId") String fileId, @Query("fileBucket") FileBucket fileBucket,
      @Query("accountId") String accountId, @Query("encrypted") boolean encrypted);

  @GET("delegateFiles/downloadConfig")
  Call<ResponseBody> downloadFile(@Query("fileId") String fileId, @Query("accountId") String accountId,
      @Query("appId") String appId, @Query("activityId") String activityId);

  @GET("delegateFiles/metainfo")
  Call<RestResponse<DelegateFile>> getMetaInfo(
      @Query("fileId") String fileId, @Query("fileBucket") FileBucket fileBucket, @Query("accountId") String accountId);

  @KryoResponse
  @PUT("delegates/{delegateId}/tasks/{taskId}/acquire")
  Call<DelegateTask> acquireTask(
      @Path("delegateId") String delegateId, @Path("taskId") String uuid, @Query("accountId") String accountId);

  @KryoResponse
  @POST("delegates/{delegateId}/tasks/{taskId}/report")
  Call<DelegateTask> reportConnectionResults(@Path("delegateId") String delegateId, @Path("taskId") String uuid,
      @Query("accountId") String accountId, @Body List<DelegateConnectionResult> results);

  @KryoResponse
  @GET("delegates/{delegateId}/tasks/{taskId}/proceed")
  Call<DelegateTask> shouldProceedAnyway(
      @Path("delegateId") String delegateId, @Path("taskId") String uuid, @Query("accountId") String accountId);

  @KryoResponse
  @PUT("delegates/{delegateId}/tasks/{taskId}/start")
  Call<DelegateTask> startTask(
      @Path("delegateId") String delegateId, @Path("taskId") String uuid, @Query("accountId") String accountId);

  @GET("delegates/{delegateId}/task-events")
  Call<List<DelegateTaskEvent>> pollTaskEvents(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId);

  @GET("delegates/{delegateId}/heartbeat")
  Call<Delegate> delegateHeartbeat(@Path("delegateId") String delegateId, @Query("accountId") String accountId);
}
