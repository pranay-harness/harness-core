package software.wings.managerclient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.beans.Delegate;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public interface ManagerClient {
  @POST("delegates/register")
  Call<Delegate> registerDelegate(@Query("accountId") String accountId, @Body Delegate delegate);

  @PUT("delegates/{delegateId}")
  Call<Delegate> sendHeartbeat(
      @Path("delegateId") String delegateId, @Query("accountId") String accountId, @Body Delegate delegate);
}
