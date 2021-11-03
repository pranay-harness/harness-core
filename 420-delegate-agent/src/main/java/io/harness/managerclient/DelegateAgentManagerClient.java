package io.harness.managerclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.rest.RestResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateAgentManagerClient {
  String BASE_URL = "manager/agent/";

  @Multipart
  @POST(BASE_URL + "delegateFiles/{delegateId}/tasks/{taskId}")
  Call<RestResponse<String>> uploadFile(@Path("delegateId") String delegateId, @Path("taskId") String taskId,
      @Query("accountId") String accountId, @Query("fileBucket") FileBucket bucket, @Part MultipartBody.Part file);

  @GET(BASE_URL + "delegateFiles/fileId")
  Call<RestResponse<String>> getFileIdByVersion(@Query("entityId") String entityId,
      @Query("fileBucket") FileBucket fileBucket, @Query("version") int version, @Query("accountId") String accountId);

  @GET(BASE_URL + "delegateFiles/download")
  Call<ResponseBody> downloadFile(
      @Query("fileId") String fileId, @Query("fileBucket") FileBucket fileBucket, @Query("accountId") String accountId);

  @GET(BASE_URL + "delegateFiles/downloadConfig")
  Call<ResponseBody> downloadFile(@Query("fileId") String fileId, @Query("accountId") String accountId,
      @Query("appId") String appId, @Query("activityId") String activityId);

  @GET(BASE_URL + "delegateFiles/metainfo")
  Call<RestResponse<DelegateFile>> getMetaInfo(
      @Query("fileId") String fileId, @Query("fileBucket") FileBucket fileBucket, @Query("accountId") String accountId);

  @POST(BASE_URL + "delegates/instance-sync/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishInstanceSyncResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body DelegateResponseData responseData);

  @POST(BASE_URL + "delegates/instance-sync-ng/{perpetualTaskId}")
  Call<RestResponse<Boolean>> processInstanceSyncNGResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body InstanceSyncPerpetualTaskResponse responseData);

  @POST("logs/activity/{activityId}/unit/{unitName}/batched")
  Call<RestResponse> saveCommandUnitLogs(@Path("activityId") String activityId, @Path("unitName") String unitName,
      @Query("accountId") String accountId, @Body RequestBody logObject);

  @POST(BASE_URL + "delegates/{delegateId}/state-executions")
  Call<RestResponse> saveApiCallLogs(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId, @Body RequestBody logObject);

  @POST(BASE_URL + "delegates/manifest-collection/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishManifestCollectionResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body RequestBody manifestCollectionExecutionResponse);

  @POST(BASE_URL + "delegates/connectors/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishConnectorHeartbeatResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body ConnectorHeartbeatDelegateResponse responseData);

  @POST(BASE_URL + "delegates/artifact-collection/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishArtifactCollectionResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body RequestBody buildSourceExecutionResponse);

  @POST(BASE_URL + "delegates/polling/{perpetualTaskId}")
  Call<RestResponse<Boolean>> publishPollingResult(@Path("perpetualTaskId") String perpetualTaskId,
      @Query("accountId") String accountId, @Body RequestBody buildSourceExecutionResponse);

  @GET("service-templates/{templateId}/compute-files")
  Call<RestResponse<String>> getConfigFiles(@Path("templateId") String templateId, @Query("accountId") String accountId,
      @Query("appId") String appId, @Query("envId") String envId, @Query("hostId") String hostId);
}
