package software.wings.helpers.ext.elk;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.Map;

/**
 * Created by sriram_parthasarathy on 10/2/17.
 */
public interface KibanaRestClient {
  String searchPathPattern = "%%2F%s%%2F_search%%3Fsize=%s";
  String searchMethod = "POST";

  @GET("app/kibana") Call<Object> version();

  @POST("api/console/proxy?path=_template&method=GET") Call<Map<String, Map<String, Object>>> template();

  @POST("api/console/proxy")
  Call<Object> getLogSample(@Query(value = "path", encoded = true) String index, @Query(value = "method") String method,
      @Body Object elkLogFetchRequest);

  @POST("api/console/proxy")
  Call<Object> search(@Query(value = "path", encoded = true) String index, @Query(value = "method") String method,
      @Body Object elkLogFetchRequest);

  @POST("api/console/proxy?path=_search%3Fsize=1&method=POST") Call<Object> validate();
}
