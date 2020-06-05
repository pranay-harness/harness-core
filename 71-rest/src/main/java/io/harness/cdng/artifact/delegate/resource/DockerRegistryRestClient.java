package io.harness.cdng.artifact.delegate.resource;

import io.harness.cdng.artifact.delegate.beans.DockerPublicImageTagResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface DockerRegistryRestClient {
  //  https://auth.docker.io/token?service=registry.docker.io&scope=repository:samalba/my-app:pull,push

  @GET("/v2/repositories/{imageName}/tags/{tagNumber}")
  Call<DockerPublicImageTagResponse.Result> getPublicImageTag(
      @Path(value = "imageName", encoded = true) String imageName,
      @Path(value = "tagNumber", encoded = true) String tagNumber);
}
