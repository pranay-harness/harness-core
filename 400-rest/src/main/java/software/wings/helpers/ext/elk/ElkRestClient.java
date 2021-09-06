package software.wings.helpers.ext.elk;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by rsingh on 8/01/17.
 */
@OwnedBy(HarnessTeam.CV)
@TargetModule(_960_API_SERVICES)
public interface ElkRestClient {
  @POST("{index}/_search")
  Call<Object> search(@Path("index") String index, @Body Object elkLogFetchRequest, @Query("size") int size);

  @GET("_template") Call<Map<String, Map<String, Object>>> template();

  @POST("{index}/_search?size=1")
  Call<Object> getLogSample(@Path("index") String index, @Body Object elkLogFetchRequest);
}
