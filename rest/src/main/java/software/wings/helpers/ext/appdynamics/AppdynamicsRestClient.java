package software.wings.helpers.ext.appdynamics;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;
import software.wings.service.impl.appdynamics.AppdynamicsApplication;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;

import java.util.List;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsRestClient {
  /**
   * Lists all the applications of appdynamics
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("rest/applications?output=json")
  Call<List<AppdynamicsApplication>> listAllApplications(@Header("Authorization") String authorization);

  /**
   * Lists all the tiers of an application in appdynamics
   *
   * @param authorization
   * @param appdynamicsAppId
   * @return
   */
  @GET("rest/applications/{appdynamicsAppId}/tiers?output=json")
  Call<List<AppdynamicsTier>> listTiers(
      @Header("Authorization") String authorization, @Path("appdynamicsAppId") long appdynamicsAppId);

  /**
   * Lists all the nodes of a tier and application in appdynamics
   *
   * @param authorization
   * @param appdynamicsAppId
   * @return
   */
  @GET("rest/applications/{appdynamicsAppId}/tiers/{tierId}/nodes?output=json")
  Call<List<AppdynamicsNode>> listNodes(@Header("Authorization") String authorization,
      @Path("appdynamicsAppId") long appdynamicsAppId, @Path("tierId") long tierId);

  /**
   * Get all the business transactions of an application
   *
   * @param authorization
   * @param appdynamicsAppId
   * @return
   */
  @GET("rest/applications/{appdynamicsAppId}/business-transactions?output=json")
  Call<List<AppdynamicsBusinessTransaction>> listBusinessTransactions(
      @Header("Authorization") String authorization, @Path("appdynamicsAppId") long appdynamicsAppId);

  /**
   * Get all the details of a tier
   *
   * @param authorization
   * @param appdynamicsAppId
   * @return
   */
  @GET("rest/applications/{appdynamicsAppId}/tiers/{tierId}?output=json")
  Call<List<AppdynamicsTier>> getTierDetails(@Header("Authorization") String authorization,
      @Path("appdynamicsAppId") long appdynamicsAppId, @Path("tierId") long tierId);

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
      @Path("applicationId") long applicationId, @Query("metric-path") String metricPath);
}
