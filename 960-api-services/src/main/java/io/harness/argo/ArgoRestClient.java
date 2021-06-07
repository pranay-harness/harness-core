package io.harness.argo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.argo.beans.AppSyncOptions;
import io.harness.argo.beans.ArgoApp;
import io.harness.argo.beans.ArgoToken;
import io.harness.argo.beans.ClusterResourceTree;
import io.harness.argo.beans.ManagedResource;
import io.harness.argo.beans.RevisionMeta;
import io.harness.argo.beans.UsernamePassword;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import java.util.List;

@OwnedBy(HarnessTeam.CDP)
public interface ArgoRestClient {
  @POST("api/v1/session") Call<ArgoToken> fetchToken(@Body UsernamePassword usernamePassword);

  @GET("api/v1/applications/{appName}")
  Call<ArgoApp> fetchApp(@Header("Authorization") String bearerAuthHeader, @Path(value = "appName") String appName);

  @GET("api/v1/applications/{appName}/managed-resources")
  Call<List<ManagedResource>> fetchResourceStates(
      @Header("Authorization") String bearerAuthHeader, @Path(value = "appName") String appName);

  @GET("api/v1/applications/{appName}/resource-tree")
  Call<ClusterResourceTree> fetchResourceTree(
      @Header("Authorization") String bearerAuthHeader, @Path(value = "appName") String appName);

  @POST("api/v1/applications")
  Call<ArgoApp> createApp(@Header("Authorization") String bearerAuthHeader, @Body ArgoApp argoApp);

  @PUT("api/v1/applications/{appName}")
  Call<ArgoApp> updateApp(
      @Header("Authorization") String bearerAuthHeader, @Path(value = "appName") String appName, @Body ArgoApp argoApp);

  @POST("api/v1/applications/{appName}/sync")
  Call<ArgoApp> syncApp(@Header("Authorization") String bearerAuthHeader, @Path(value = "appName") String appName,
      @Body AppSyncOptions syncOptions);

  @GET("api/v1/applications/{appName}/revisions/{revision}/metadata")
  Call<RevisionMeta> revisionMeta(@Header("Authorization") String bearerAuthHeader,
      @Path(value = "appName") String appName, @Path("revision") String revision);
}
