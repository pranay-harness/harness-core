package software.wings.helpers.ext.elk;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import software.wings.service.impl.elk.ElkAuthenticationResponse;

/**
 * Created by rsingh on 8/01/17.
 */
public interface ElkRestClient {
  @GET("_xpack/security/_authenticate")
  Call<ElkAuthenticationResponse> authenticate(@Header("Authorization") String authorization);

  @POST("_search") Call<Object> search(@Body String elkLogFetchRequest);
}
