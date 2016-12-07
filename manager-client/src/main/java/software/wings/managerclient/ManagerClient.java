package software.wings.managerclient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.beans.Delegate;
import software.wings.beans.RestResponse;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public interface ManagerClient {
  @POST("delegates/register")
  Call<RestResponse<Delegate>> registerDelegate(@Query("accountId") String accountId, @Body Delegate delegate);

  @PUT("delegates/{delegateId}")
  Call<RestResponse<Delegate>> sendHeartbeat(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId, @Body Delegate delegate);
}
