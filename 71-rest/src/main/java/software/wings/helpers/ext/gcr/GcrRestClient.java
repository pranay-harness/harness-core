package software.wings.helpers.ext.gcr;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import software.wings.helpers.ext.gcr.GcrServiceImpl.GcrImageTagResponse;

/**
 * Created by brett on 8/2/17
 */
public interface GcrRestClient {
  @GET("/v2/{imageName}/tags/list")
  Call<GcrImageTagResponse> listImageTags(
      @Header("Authorization") String bearerAuthHeader, @Path(value = "imageName", encoded = true) String imageName);

  @GET("/v2/_catalog")
  Call<GcrImageTagResponse> listCatalogs(
      @Header("Authorization") String bearerAuthHeader, @Path(value = "imageName", encoded = true) String imageName);
}
