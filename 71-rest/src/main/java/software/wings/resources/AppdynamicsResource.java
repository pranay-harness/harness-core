package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
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

  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Inject private LearningEngineService learningEngineService;

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
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId,
      @Valid AppdynamicsSetupTestNodeData appdynamicsSetupTestNodeData) {
    return new RestResponse<>(appdynamicsService.getMetricsWithDataForNode(appdynamicsSetupTestNodeData));
  }
}
