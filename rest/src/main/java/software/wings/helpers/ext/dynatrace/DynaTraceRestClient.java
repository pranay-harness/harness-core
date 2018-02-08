package software.wings.helpers.ext.dynatrace;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataRequest;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataResponse;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;

import java.util.List;

/**
 * Created by rsingh on 1/29/18.
 */
public interface DynaTraceRestClient {
  @GET("api/v1/timeseries")
  Call<List<DynaTraceTimeSeries>> listTimeSeries(@Header("Authorization") String authorization);

  @POST("api/v1/timeseries")
  Call<DynaTraceMetricDataResponse> fetchMetricData(
      @Header("Authorization") String authorization, @Body DynaTraceMetricDataRequest request);
}
