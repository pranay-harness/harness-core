package software.wings.helpers.ext.instana;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import software.wings.service.impl.instana.InstanaAnalyzeMetricRequest;
import software.wings.service.impl.instana.InstanaAnalyzeMetrics;
import software.wings.service.impl.instana.InstanaInfraMetricRequest;
import software.wings.service.impl.instana.InstanaInfraMetrics;

public interface InstanaRestClient {
  @POST("api/infrastructure-monitoring/metrics/")
  Call<InstanaInfraMetrics> getInfrastructureMetrics(
      @Header("Authorization") String authorization, @Body InstanaInfraMetricRequest request);
  @POST("api/application-monitoring/analyze/trace-groups")
  Call<InstanaAnalyzeMetrics> getGroupedTraceMetrics(
      @Header("Authorization") String authorization, @Body InstanaAnalyzeMetricRequest request);
}
