package software.wings.managerclient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.RestResponse;
import software.wings.dl.PageResponse;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public interface ManagerClient {
  @POST("delegates/register")
  Call<RestResponse<Delegate>> registerDelegate(@Query("accountId") String accountId, @Body Delegate delegate);

  @PUT("delegates/{delegateId}")
  Call<RestResponse<Delegate>> sendHeartbeat(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId, @Body Delegate delegate);

  @GET("delegates/{delegateId}/tasks")
  Call<RestResponse<PageResponse<DelegateTask>>> getTasks(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId);

  @POST("delegates/{delegateId}/tasks/{taskId}")
  Call<Void> sendTaskStatus(@Path("delegateId") String delegateId, @Path("taskId") String taskId,
      @Query("accountId") String accountId, @Body DelegateTaskResponse delegateTaskResponse);
}
