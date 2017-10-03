package software.wings.helpers.ext.elk;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.Map;

/**
 * Created by rsingh on 8/01/17.
 */
public interface ElkRestClient {
  @POST("{index}/_search?size=10000") Call<Object> search(@Path("index") String index, @Body Object elkLogFetchRequest);

  @GET("_template") Call<Map<String, Map<String, Object>>> template();

  @POST("{index}/_search?size=1")
  Call<Object> getLogSample(@Path("index") String index, @Body Object elkLogFetchRequest);
}
