package software.wings.helpers.ext.nexus;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import software.wings.helpers.ext.nexus.model.DockerImageResponse;
import software.wings.helpers.ext.nexus.model.DockerImageTagResponse;
import software.wings.helpers.ext.nexus.model.Nexus3Repository;
import software.wings.helpers.ext.nexus.model.RepositoryRequest;
import software.wings.helpers.ext.nexus.model.RepositoryResponse;

import java.util.List;

/**
 * Created by sgurubelli on 11/16/17.
 */
public interface NexusThreeRestClient {
  @Headers("Accept: application/json")
  @POST("service/extdirect")
  Call<RepositoryResponse> getRepositories(
      @Header("Authorization") String authorization, @Body RepositoryRequest repositoryRequest);

  @Headers("Accept: application/json")
  @POST("service/extdirect")
  Call<RepositoryResponse> getRepositories(@Body RepositoryRequest repositoryRequest);

  @GET("repository/{repoKey}/v2/_catalog")
  Call<DockerImageResponse> getDockerImages(
      @Header("Authorization") String authorization, @Path(value = "repoKey", encoded = true) String repository);

  @GET("repository/{repoKey}/v2/_catalog")
  Call<DockerImageResponse> getDockerImages(@Path(value = "repoKey", encoded = true) String repository);

  @GET("repository/{repoKey}/v2/{imageName}/tags/list")
  Call<DockerImageTagResponse> getDockerTags(@Header("Authorization") String authorization,
      @Path(value = "repoKey", encoded = true) String repository,
      @Path(value = "imageName", encoded = true) String imageName);

  @GET("repository/{repoKey}/v2/{imageName}/tags/list")
  Call<DockerImageTagResponse> getDockerTags(@Path(value = "repoKey", encoded = true) String repository,
      @Path(value = "imageName", encoded = true) String imageName);

  @Headers("Accept: application/json")
  @GET("/service/rest/v1/repositories")
  Call<List<Nexus3Repository>> listRepositories(@Header("Authorization") String authorization);

  @Headers("Accept: application/json")
  @GET("/service/rest/v1/repositories")
  Call<List<Nexus3Repository>> listRepositories();
}
