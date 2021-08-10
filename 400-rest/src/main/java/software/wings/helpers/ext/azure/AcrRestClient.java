package software.wings.helpers.ext.azure;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(CDC)
@TargetModule(_960_API_SERVICES)
public interface AcrRestClient {
  @GET("/v2/_catalog")
  Call<AcrGetRepositoriesResponse> listRepositories(
      @Header("Authorization") String basicAuthHeader, @Query("last") String last);

  @GET("/v2/{repositoryName}/tags/list?n=500&orderby=timedesc")
  Call<AcrGetRepositoryTagsResponse> listRepositoryTags(@Header("Authorization") String basicAuthHeader,
      @Path(value = "repositoryName", encoded = true) String repositoryName);
}
