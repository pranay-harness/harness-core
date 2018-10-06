package software.wings.managerclient;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.beans.DelegateConfiguration;
import software.wings.beans.DelegateScripts;
import software.wings.beans.RestResponse;

public interface ManagerClient {
  @GET("delegates/delegateScripts")
  Call<RestResponse<DelegateScripts>> getDelegateScripts(
      @Query("accountId") String accountId, @Query("delegateVersion") String delegateVersion);

  @GET("delegates/configuration")
  Call<RestResponse<DelegateConfiguration>> getDelegateConfiguration(@Query("accountId") String accountId);

  @GET("infra-download/delegate-auth/delegate/{version}")
  Call<RestResponse<String>> getDelegateDownloadUrl(
      @Path("version") String version, @Query("accountId") String accountId);

  @GET("infra-download/delegate-auth/watcher/{version}")
  Call<RestResponse<String>> getWatcherDownloadUrl(
      @Path("version") String version, @Query("accountId") String accountId);

  @GET("account/{accountId}/status") Call<RestResponse<String>> getAccountStatus(@Path("accountId") String accountId);
}
