package software.wings.helpers.ext.elk;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import software.wings.service.impl.elk.ElkAuthenticationResponse;

import java.util.Map;

/**
 * Created by rsingh on 8/01/17.
 */
public interface ElkRestClient {
  @GET("_xpack/security/_authenticate")
  Call<ElkAuthenticationResponse> authenticate(@Header("Authorization") String authorization);

  @POST("_search?size=10000") Call<Object> search(@Body Object elkLogFetchRequest);

  @GET("_template") Call<Map<String, Map<String, Object>>> template();

  @POST("_search?size=1") Call<Object> getLogSample(@Body Object elkLogFetchRequest);
}
