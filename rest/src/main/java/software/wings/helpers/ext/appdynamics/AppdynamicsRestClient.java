package software.wings.helpers.ext.appdynamics;

import com.fasterxml.jackson.databind.JsonNode;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.service.impl.appdynamics.AppdynamicsApplicationResponse;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;

import java.util.List;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsRestClient {
  /**
   * List all the applications of appdynamics
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("rest/applications?output=json")
  Call<List<AppdynamicsApplicationResponse>> listAllApplications(@Header("Authorization") String authorization);

  /**
   * List the parent metrices of appdynamics application
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("rest/applications/{applicationId}/metrics?output=JSON")
  Call<List<AppdynamicsMetric>> listParentMetrics(
      @Header("Authorization") String authorization, @Path("applicationId") int applicationId);

  /**
   * List the metrices of appdynamics application for a give path
   *
   * @param authorization the authorization
   * @param metricPath    the path to look for metrices
   * @return the call
   */
  @GET("rest/applications/{applicationId}/metrics?output=JSON")
  Call<List<AppdynamicsMetric>> listMetrices(@Header("Authorization") String authorization,
      @Path("applicationId") int applicationId, @Query("metric-path") String metricPath);
}
