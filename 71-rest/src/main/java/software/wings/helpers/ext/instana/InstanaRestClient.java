package software.wings.helpers.ext.instana;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import software.wings.service.impl.instana.InstanaInfraMetricRequest;
import software.wings.service.impl.instana.InstanaInfraMetrics;

public interface InstanaRestClient {
  @POST("api/infrastructure-monitoring/metrics/")
  Call<InstanaInfraMetrics> getInfrastructureMetrics(
      @Header("Authorization") String authorization, @Body InstanaInfraMetricRequest request);
}
