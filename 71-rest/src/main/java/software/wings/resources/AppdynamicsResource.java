package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/14/17.
 *
 * For api versioning see documentation of {@link NewRelicResource}.
 *
 */
@Api("appdynamics")
@Path("/appdynamics")
@Produces("application/json")
@Scope(SETTING)
public class AppdynamicsResource {
  @Inject private AppdynamicsService appdynamicsService;

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicApplication>> getAllApplications(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId) throws IOException {
    return new RestResponse<>(appdynamicsService.getApplications(settingId));
  }

  @GET
  @Path("/tiers")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<AppdynamicsTier>> getAllTiers(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("appdynamicsAppId") long appdynamicsAppId)
      throws IOException {
    return new RestResponse<>(appdynamicsService.getTiers(settingId, appdynamicsAppId));
  }

  @GET
  @Path("/dependent-tiers")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<AppdynamicsTier>> getDependentTiers(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("appdynamicsAppId") long appdynamicsAppId,
      @QueryParam("tierId") long tierId, @QueryParam("tierName") String tierName) throws IOException {
    return new RestResponse<>(appdynamicsService.getDependentTiers(
        settingId, appdynamicsAppId, AppdynamicsTier.builder().id(tierId).name(tierName).build()));
  }

  /**
   * Api to fetch Metric data for given node.
   * @param accountId
   * @param appdynamicsSetupTestNodeData
   * @return
   */
  @POST
  @Path("/node-data")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId,
      @Valid AppdynamicsSetupTestNodeData appdynamicsSetupTestNodeData) {
    return new RestResponse<>(appdynamicsService.getMetricsWithDataForNode(appdynamicsSetupTestNodeData));
  }

  @POST
  @Path("/metric-data")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<AppdynamicsValidationResponse>> getMetricData(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("projectId") @NotNull String projectId,
      @QueryParam("connectorId") @NotNull String connectorId, @QueryParam("appdAppId") @NotNull long appdAppId,
      @QueryParam("appdTierId") @NotNull long appdTierId, @QueryParam("requestGuid") @NotNull String requestGuid,
      @NotNull @Valid @Body List<MetricPack> metricPacks) {
    return new RestResponse<>(appdynamicsService.getMetricPackData(
        accountId, projectId, connectorId, appdAppId, appdTierId, requestGuid, metricPacks));
  }
}
