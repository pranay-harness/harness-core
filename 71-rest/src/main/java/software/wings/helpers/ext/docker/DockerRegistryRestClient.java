package software.wings.helpers.ext.docker;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl.DockerImageTagResponse;
import software.wings.helpers.ext.docker.DockerRegistryServiceImpl.DockerRegistryToken;

public interface DockerRegistryRestClient {
  //  https://auth.docker.io/token?service=registry.docker.io&scope=repository:samalba/my-app:pull,push

  @GET
  Call<DockerRegistryToken> getToken(@Header("Authorization") String basicAuthHeader, @Url String url,
      @Query("service") String service, @Query("scope") String scope);

  @GET("/v2/{imageName}/tags/list")
  Call<DockerImageTagResponse> listImageTags(
      @Header("Authorization") String bearerAuthHeader, @Path(value = "imageName", encoded = true) String imageName);

  @GET("/v2") Call<Object> getApiVersion(@Header("Authorization") String bearerAuthHeader);

  @GET
  Call<DockerImageTagResponse> listImageTagsByUrl(@Header("Authorization") String bearerAuthHeader, @Url String url);
}
