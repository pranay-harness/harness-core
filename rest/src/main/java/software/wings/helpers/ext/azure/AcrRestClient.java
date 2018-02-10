package software.wings.helpers.ext.azure;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface AcrRestClient {
  @GET("/v2/_catalog")
  Call<AcrGetRepositoriesResponse> listRepositories(@Header("Authorization") String basicAuthHeader);

  @GET("/v2/{repositoryName}/tags/list")
  Call<AcrGetRepositoryTagsResponse> listRepositoryTags(@Header("Authorization") String basicAuthHeader,
      @Path(value = "repositoryName", encoded = true) String repositoryName);
}
