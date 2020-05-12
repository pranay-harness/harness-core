package software.wings.helpers.ext.nexus;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.helpers.ext.nexus.model.DockerImageResponse;
import software.wings.helpers.ext.nexus.model.DockerImageTagResponse;
import software.wings.helpers.ext.nexus.model.Nexus3AssetResponse;
import software.wings.helpers.ext.nexus.model.Nexus3ComponentResponse;
import software.wings.helpers.ext.nexus.model.Nexus3Repository;
import software.wings.helpers.ext.nexus.model.RepositoryRequest;
import software.wings.helpers.ext.nexus.model.RepositoryResponse;

import java.util.List;

/**
 * Created by sgurubelli on 11/16/17.
 */
@OwnedBy(CDC)
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
  @GET("service/rest/v1/repositories")
  Call<List<Nexus3Repository>> listRepositories(@Header("Authorization") String authorization);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/repositories")
  Call<List<Nexus3Repository>> listRepositories();

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> search(@Header("Authorization") String authorization,
      @Query("repository") String repository, @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> search(
      @Query("repository") String repository, @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> getPackageVersions(@Header("Authorization") String authorization,
      @Query("repository") String repository, @Query("name") String packageName,
      @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> getPackageVersions(@Query("repository") String repository,
      @Query("name") String packageName, @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search/assets")
  Call<Nexus3AssetResponse> getAsset(@Header("Authorization") String authorization,
      @Query("repository") String repository, @Query("name") String name, @Query("version") String version);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search/assets")
  Call<Nexus3AssetResponse> getAsset(
      @Query("repository") String repository, @Query("name") String name, @Query("version") String version);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> getArtifactNames(@Header("Authorization") String authorization,
      @Query("repository") String repository, @Query("maven.groupId") String groupId,
      @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> getArtifactNames(@Query("repository") String repository,
      @Query("maven.groupId") String groupId, @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> getArtifactVersions(@Header("Authorization") String authorization,
      @Query("repository") String repository, @Query("maven.groupId") String groupId,
      @Query("maven.artifactId") String artifactId, @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> getArtifactVersions(@Query("repository") String repository,
      @Query("maven.groupId") String groupId, @Query("maven.artifactId") String artifactId,
      @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> getArtifactVersionsWithExtensionAndClassifier(
      @Header("Authorization") String authorization, @Query("repository") String repository,
      @Query("maven.groupId") String groupId, @Query("maven.artifactId") String artifactId,
      @Query("maven.extension") String extension, @Query("maven.classifier") String classifier,
      @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search")
  Call<Nexus3ComponentResponse> getArtifactVersionsWithExtensionAndClassifier(@Query("repository") String repository,
      @Query("maven.groupId") String groupId, @Query("maven.artifactId") String artifactId,
      @Query("maven.extension") String extension, @Query("maven.classifier") String classifier,
      @Query("continuationToken") String continuationToken);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search/assets")
  Call<Nexus3AssetResponse> getMavenAsset(@Header("Authorization") String authorization,
      @Query("repository") String repository, @Query("maven.groupId") String groupId,
      @Query("maven.artifactId") String artifactId, @Query("version") String version);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search/assets")
  Call<Nexus3AssetResponse> getMavenAsset(@Query("repository") String repository,
      @Query("maven.groupId") String groupId, @Query("maven.artifactId") String artifactId,
      @Query("version") String version);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search/assets")
  Call<Nexus3AssetResponse> getMavenAssetWithExtensionAndClassifier(@Header("Authorization") String authorization,
      @Query("repository") String repository, @Query("maven.groupId") String groupId,
      @Query("maven.artifactId") String artifactId, @Query("version") String version,
      @Query("maven.extension") String extension, @Query("maven.classifier") String classifier);

  @Headers("Accept: application/json")
  @GET("service/rest/v1/search/assets")
  Call<Nexus3AssetResponse> getMavenAssetWithExtensionAndClassifier(@Query("repository") String repository,
      @Query("maven.groupId") String groupId, @Query("maven.artifactId") String artifactId,
      @Query("version") String version, @Query("maven.extension") String extension,
      @Query("maven.classifier") String classifier);
}
