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
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.delegatetasks.DelegateFile;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.List;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public interface ManagerClient {
  @POST("delegates/register")
  Call<RestResponse<Delegate>> registerDelegate(@Query("accountId") String accountId, @Body Delegate delegate);

  @PUT("delegates/{delegateId}")
  Call<RestResponse<Delegate>> sendHeartbeat(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId, @Body Delegate delegate);

  @Headers({"Accept: application/x-kryo"})
  @KryoResponse
  @GET("delegates/{delegateId}/tasks")
  Call<RestResponse<PageResponse<DelegateTask>>> getTasks(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId);

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
  Call<RestResponse<Delegate>> checkForUpgrade(
      @Header("Version") String version, @Path("delegateId") String delegateId, @Query("accountId") String accountId);

  @GET("service-templates/{templateId}/compute-files")
  Call<RestResponse<List<ConfigFile>>> getConfigFiles(@Path("templateId") String templateId,
      @Query("accountId") String accountId, @Query("appId") String appId, @Query("envId") String envId,
      @Query("hostName") String hostName);

  @POST("logs") Call<RestResponse<Log>> saveLog(@Query("accountId") String accountId, @Body Log log);

  @GET("delegateFiles/fileId")
  Call<RestResponse<String>> getFileIdByVersion(@Query("entityId") String entityId,
      @Query("fileBucket") FileBucket fileBucket, @Query("version") int version, @Query("accountId") String accountId);

  @GET("delegateFiles/download")
  Call<ResponseBody> downloadFile(
      @Query("fileId") String fileId, @Query("fileBucket") FileBucket fileBucket, @Query("accountId") String accountId);

  @GET("delegateFiles/metainfo")
  Call<RestResponse<DelegateFile>> getMetaInfo(
      @Query("fileId") String fileId, @Query("fileBucket") FileBucket fileBucket, @Query("accountId") String accountId);
}
