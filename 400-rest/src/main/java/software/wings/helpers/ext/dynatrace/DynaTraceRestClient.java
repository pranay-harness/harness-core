package software.wings.helpers.ext.dynatrace;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.dynatrace.DynaTraceApplication;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataRequest;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by rsingh on 1/29/18.
 */
@OwnedBy(HarnessTeam.CV)
@TargetModule(_960_API_SERVICES)
public interface DynaTraceRestClient {
  @GET("api/v1/timeseries") Call<Object> listTimeSeries(@Header("Authorization") String authorization);

  @GET("api/v1/entity/services")
  Call<List<DynaTraceApplication>> getServices(@Header("Authorization") String authorization,
      @Query("pageSize") int pageSize, @Query("nextPageKey") String nextPageKey);

  @POST("api/v1/timeseries")
  Call<DynaTraceMetricDataResponse> fetchMetricData(
      @Header("Authorization") String authorization, @Body DynaTraceMetricDataRequest request);
}
