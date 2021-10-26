package io.harness.managerclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.logging.AccessTokenBean;
import io.harness.rest.RestResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateAgentManagerClient {
  @Multipart
  @POST("agent/delegateFiles/{delegateId}/tasks/{taskId}")
  Call<RestResponse<String>> uploadFile(@Path("delegateId") String delegateId, @Path("taskId") String taskId,
      @Query("accountId") String accountId, @Query("fileBucket") FileBucket bucket, @Part MultipartBody.Part file);

  @GET("agent/delegateFiles/fileId")
  Call<RestResponse<String>> getFileIdByVersion(@Query("entityId") String entityId,
      @Query("fileBucket") FileBucket fileBucket, @Query("version") int version, @Query("accountId") String accountId);

  @GET("agent/delegateFiles/download")
  Call<ResponseBody> downloadFile(
      @Query("fileId") String fileId, @Query("fileBucket") FileBucket fileBucket, @Query("accountId") String accountId);

  @GET("agent/delegateFiles/downloadConfig")
  Call<ResponseBody> downloadFile(@Query("fileId") String fileId, @Query("accountId") String accountId,
      @Query("appId") String appId, @Query("activityId") String activityId);

  @GET("agent/delegateFiles/metainfo")
  Call<RestResponse<DelegateFile>> getMetaInfo(
      @Query("fileId") String fileId, @Query("fileBucket") FileBucket fileBucket, @Query("accountId") String accountId);

  @GET("agent/dms/delegates/delegateScripts")
  Call<RestResponse<DelegateScripts>> getDelegateScripts(@Query("accountId") String accountId,
      @Query("delegateVersion") String delegateVersion, @Query("delegateName") String delegateName);

  // todo(abhinav): discuss with raghu
  @GET("agent/infra-download/delegate-auth/delegate/logging-token")
  Call<RestResponse<AccessTokenBean>> getLoggingToken(@Query("accountId") String accountId);

  @POST("agent/delegates/instance-sync/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishInstanceSyncResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body DelegateResponseData responseData);

  @POST("agent/delegates/instance-sync-ng/{perpetualTaskId}")
  Call<RestResponse<Boolean>> processInstanceSyncNGResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body InstanceSyncPerpetualTaskResponse responseData);

  @POST("logs/activity/{activityId}/unit/{unitName}/batched")
  Call<RestResponse> saveCommandUnitLogs(@Path("activityId") String activityId, @Path("unitName") String unitName,
      @Query("accountId") String accountId, @Body RequestBody logObject);

  @POST("agent/delegates/{delegateId}/state-executions")
  Call<RestResponse> saveApiCallLogs(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId, @Body RequestBody logObject);

  @POST("agent/delegates/manifest-collection/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishManifestCollectionResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body RequestBody manifestCollectionExecutionResponse);

  @POST("agent/delegates/connectors/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishConnectorHeartbeatResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body ConnectorHeartbeatDelegateResponse responseData);

  @POST("agent/delegates/artifact-collection/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishArtifactCollectionResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body RequestBody buildSourceExecutionResponse);

  @POST("agent/delegates/polling/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishPollingResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body RequestBody buildSourceExecutionResponse);

  @GET("service-templates/{templateId}/compute-files")
  Call<RestResponse<String>> getConfigFiles(@Path("templateId") String templateId, @Query("accountId") String accountId,
      @Query("appId") String appId, @Query("envId") String envId, @Query("hostId") String hostId);
}
