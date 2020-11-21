package software.wings.helpers.ext.gcr;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.helpers.ext.gcr.GcrServiceImpl.GcrImageTagResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

/**
 * Created by brett on 8/2/17
 */
@OwnedBy(CDC)
public interface GcrRestClient {
  @GET("/v2/{imageName}/tags/list")
  Call<GcrImageTagResponse> listImageTags(
      @Header("Authorization") String bearerAuthHeader, @Path(value = "imageName", encoded = true) String imageName);

  @GET("/v2/_catalog")
  Call<GcrImageTagResponse> listCatalogs(
      @Header("Authorization") String bearerAuthHeader, @Path(value = "imageName", encoded = true) String imageName);
}
