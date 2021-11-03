package io.harness.dmsclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateHeartbeatResponse;
import io.harness.beans.DelegateTaskEventsResponse;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.task.validation.DelegateConnectionResultDetail;
import io.harness.logging.AccessTokenBean;
import io.harness.rest.RestResponse;
import io.harness.serializer.kryo.KryoRequest;
import io.harness.serializer.kryo.KryoResponse;

import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateAgentDmsClient {
  @POST("agent/delegates/register")
  Call<RestResponse<DelegateRegisterResponse>> registerDelegate(
      @Query("accountId") String accountId, @Body DelegateParams delegateParams);

  @POST("agent/delegates/connectionHeartbeat/{delegateId}")
  Call<RestResponse> doConnectionHeartbeat(@Path("delegateId") String delegateId, @Query("accountId") String accountId,
      @Body DelegateConnectionHeartbeat heartbeat);

  @Headers({"Content-Type: application/x-kryo"})
  @KryoRequest
  @POST("agent/tasks/{taskId}/delegates/{delegateId}")
  Call<ResponseBody> sendTaskStatus(@Path("delegateId") String delegateId, @Path("taskId") String taskId,
      @Query("accountId") String accountId, @Body DelegateTaskResponse delegateTaskResponse);

  @GET("agent/delegates/{delegateId}/profile")
  Call<RestResponse<DelegateProfileParams>> checkForProfile(@Path("delegateId") String delegateId,
      @Query("accountId") String accountId, @Query("profileId") String profileId,
      @Query("lastUpdatedAt") Long lastUpdatedAt);

  @GET("agent/delegates/{delegateId}/task-events")
  Call<DelegateTaskEventsResponse> pollTaskEvents(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId);

  @Multipart
  @POST("agent/delegateFiles/{delegateId}/profile-result")
  Call<RestResponse> saveProfileResult(@Path("delegateId") String delegateId, @Query("accountId") String accountId,
      @Query("error") boolean error, @Query("fileBucket") FileBucket bucket, @Part MultipartBody.Part file);

  @POST("agent/delegates/heartbeat-with-polling")
  Call<RestResponse<DelegateHeartbeatResponse>> delegateHeartbeat(
      @Query("accountId") String accountId, @Body DelegateParams delegateParams);

  // Query for a specific set of delegate properties for a given account.
  // Request: GetDelegatePropertiesRequest
  // Response: GetDelegatePropertiesResponse
  @POST("agent/delegates/properties")
  Call<RestResponse<String>> getDelegateProperties(@Query("accountId") String accountId, @Body RequestBody request);

  @KryoResponse
  @PUT("agent/delegates/{delegateId}/tasks/{taskId}/acquire")
  Call<DelegateTaskPackage> acquireTask(
      @Path("delegateId") String delegateId, @Path("taskId") String uuid, @Query("accountId") String accountId);

  @KryoResponse
  @POST("agent/delegates/{delegateId}/tasks/{taskId}/report")
  Call<DelegateTaskPackage> reportConnectionResults(@Path("delegateId") String delegateId, @Path("taskId") String uuid,
      @Query("accountId") String accountId, @Body List<DelegateConnectionResultDetail> results);

  @KryoResponse
  @GET("agent/delegates/{delegateId}/tasks/{taskId}/fail")
  Call<RestResponse> failIfAllDelegatesFailed(@Path("delegateId") String delegateId, @Path("taskId") String uuid,
      @Query("accountId") String accountId, @Query("areClientToolsInstalled") boolean areClientToolsInstalled);

  @GET("agent/delegates/delegateScripts")
  Call<RestResponse<DelegateScripts>> getDelegateScripts(@Query("accountId") String accountId,
      @Query("delegateVersion") String delegateVersion, @Query("delegateName") String delegateName);

  @GET("agent/infra-download/delegate-auth/delegate/logging-token")
  Call<RestResponse<AccessTokenBean>> getLoggingToken(@Query("accountId") String accountId);
}
