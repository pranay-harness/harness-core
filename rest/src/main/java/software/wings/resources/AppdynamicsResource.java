package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.metrics.BucketData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.impl.appdynamics.AppdynamicsApplication;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.intfc.appdynamics.AppdynamicsService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/14/17.
 */
@Api("appdynamics")
@Path("/appdynamics")
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class AppdynamicsResource {
  @Inject private AppdynamicsService appdynamicsService;

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AppdynamicsApplication>> getAllApplications(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId) throws IOException {
    return new RestResponse<>(appdynamicsService.getApplications(settingId));
  }

  @GET
  @Path("/tiers")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AppdynamicsTier>> getAllTiers(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("appdynamicsAppId") int appdynamicsAppId)
      throws IOException {
    return new RestResponse<>(appdynamicsService.getTiers(settingId, appdynamicsAppId));
  }

  @GET
  @Path("/nodes")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AppdynamicsNode>> getAllNodes(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("appdynamicsAppId") int appdynamicsAppId,
      @QueryParam("tierId") int tierId) throws IOException {
    return new RestResponse<>(appdynamicsService.getNodes(settingId, appdynamicsAppId, tierId));
  }

  @GET
  @Path("/business-transactions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AppdynamicsBusinessTransaction>> getAllBusinessTransactions(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId,
      @QueryParam("appdynamicsAppId") int appdynamicsAppId) throws IOException {
    return new RestResponse<>(appdynamicsService.getBusinessTransactions(settingId, appdynamicsAppId));
  }

  @GET
  @Path("/tier-bt-metrics")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AppdynamicsMetric>> getTierBTMetrics(@QueryParam("settingId") final String settingId,
      @QueryParam("appdynamicsAppId") int appdynamicsAppId, @QueryParam("tierId") int tierId)
      throws IOException, InterruptedException {
    return new RestResponse<>(appdynamicsService.getTierBTMetrics(settingId, appdynamicsAppId, tierId));
  }

  @GET
  @Path("/get-metric-data")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AppdynamicsMetricData>> getTierBTMetricData(@QueryParam("settingId") final String settingId,
      @QueryParam("appdynamicsAppId") int appdynamicsAppId, @QueryParam("tierId") int tierId,
      @QueryParam("btName") String btName, @QueryParam("duration-in-mins") int durationInMinutes) throws IOException {
    return new RestResponse<>(
        appdynamicsService.getTierBTMetricData(settingId, appdynamicsAppId, tierId, btName, durationInMinutes));
  }

  @POST
  @Path("/save-metrics")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricData(@QueryParam("accountId") final String accountId,
      @QueryParam("appdynamicsAppId") int appdynamicsAppId, @QueryParam("tierId") int tierId,
      List<AppdynamicsMetricData> metricData) throws IOException {
    return new RestResponse<>(appdynamicsService.saveMetricData(accountId, appdynamicsAppId, tierId, metricData));
  }

  @GET
  @Path("/generate-metrics")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, Map<String, BucketData>>> generateMetrics(
      @QueryParam("stateExecutionId") final String stateExecutionId, @QueryParam("accountId") final String accountId,
      @QueryParam("appId") final String appId) throws IOException {
    return new RestResponse<>(appdynamicsService.generateMetrics(stateExecutionId, accountId, appId));
  }

  @GET
  @Path("/get-completed-metrics")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, Map<String, BucketData>>> retrieveCompletedMetrics(
      @QueryParam("stateExecutionId") final String stateExecutionInstanceId,
      @QueryParam("accountId") final String accountId) {
    return new RestResponse<>(appdynamicsService.retrieveCompletedMetrics(stateExecutionInstanceId, accountId));
  }
}
