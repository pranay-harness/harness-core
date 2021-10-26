package io.harness.dmsclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateHeartbeatResponse;
import io.harness.beans.DelegateTaskEventsResponse;
import io.harness.delegate.beans.*;
import io.harness.delegate.task.validation.DelegateConnectionResultDetail;
import io.harness.rest.RestResponse;
import io.harness.serializer.kryo.KryoRequest;
import io.harness.serializer.kryo.KryoResponse;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateAgentDmsClient {
  @POST("agent/dms/delegates/register")
  Call<RestResponse<DelegateRegisterResponse>> registerDelegate(
      @Query("accountId") String accountId, @Body DelegateParams delegateParams);

  @POST("agent/dms/delegates/connectionHeartbeat/{delegateId}")
  Call<RestResponse> doConnectionHeartbeat(@Path("delegateId") String delegateId, @Query("accountId") String accountId,
      @Body DelegateConnectionHeartbeat heartbeat);

  @Headers({"Content-Type: application/x-kryo"})
  @KryoRequest
  @POST("agent/dms/tasks/{taskId}/delegates/{delegateId}")
  Call<ResponseBody> sendTaskStatus(@Path("delegateId") String delegateId, @Path("taskId") String taskId,
      @Query("accountId") String accountId, @Body DelegateTaskResponse delegateTaskResponse);

  @GET("agent/dms/delegates/{delegateId}/profile")
  Call<RestResponse<DelegateProfileParams>> checkForProfile(@Path("delegateId") String delegateId,
      @Query("accountId") String accountId, @Query("profileId") String profileId,
      @Query("lastUpdatedAt") Long lastUpdatedAt);

  @GET("agent/dms/delegates/{delegateId}/task-events")
  Call<DelegateTaskEventsResponse> pollTaskEvents(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId);

  @Multipart
  @POST("agent/dms/delegateFiles/{delegateId}/profile-result")
  Call<RestResponse> saveProfileResult(@Path("delegateId") String delegateId, @Query("accountId") String accountId,
      @Query("error") boolean error, @Query("fileBucket") FileBucket bucket, @Part MultipartBody.Part file);

  @POST("agent/dms/delegates/heartbeat-with-polling")
  Call<RestResponse<DelegateHeartbeatResponse>> delegateHeartbeat(
      @Query("accountId") String accountId, @Body DelegateParams delegateParams);

  // Query for a specific set of delegate properties for a given account.
  // Request: GetDelegatePropertiesRequest
  // Response: GetDelegatePropertiesResponse
  @POST("agent/dms/delegates/properties")
  Call<RestResponse<String>> getDelegateProperties(@Query("accountId") String accountId, @Body RequestBody request);

  @KryoResponse
  @PUT("agent/dms/delegates/{delegateId}/tasks/{taskId}/acquire")
  Call<DelegateTaskPackage> acquireTask(
      @Path("delegateId") String delegateId, @Path("taskId") String uuid, @Query("accountId") String accountId);

  @KryoResponse
  @POST("agent/dms/delegates/{delegateId}/tasks/{taskId}/report")
  Call<DelegateTaskPackage> reportConnectionResults(@Path("delegateId") String delegateId, @Path("taskId") String uuid,
      @Query("accountId") String accountId, @Body List<DelegateConnectionResultDetail> results);

  @KryoResponse
  @GET("agent/dms/delegates/{delegateId}/tasks/{taskId}/fail")
  Call<RestResponse> failIfAllDelegatesFailed(@Path("delegateId") String delegateId, @Path("taskId") String uuid,
      @Query("accountId") String accountId, @Query("areClientToolsInstalled") boolean areClientToolsInstalled);
}
