package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.beans.appd.AppdynamicsMetricPackDataValidationRequest;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
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
import retrofit2.http.Body;

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
  @LearningEngineAuth
  @ExposeInternalException(withStackTrace = true)
  public RestResponse<Set<AppdynamicsValidationResponse>> getMetricData(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("appdAppId") @NotNull long appdAppId, @QueryParam("appdTierId") @NotNull long appdTierId,
      @QueryParam("requestGuid") @NotNull String requestGuid,
      @NotNull @Valid @Body AppdynamicsMetricPackDataValidationRequest validationRequest) {
    return new RestResponse<>(appdynamicsService.getMetricPackData(
        accountId, orgIdentifier, projectIdentifier, appdAppId, appdTierId, requestGuid, validationRequest));
  }

  @POST
  @Path("/applications-ng")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ExposeInternalException(withStackTrace = true)
  public RestResponse<List<AppDynamicsApplication>> getAllApplications(@QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    return new RestResponse<>(
        appdynamicsService.getApplications(appDynamicsConnectorDTO, orgIdentifier, projectIdentifier));
  }

  @POST
  @Path("/tiers-ng")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ExposeInternalException(withStackTrace = true)
  public RestResponse<Set<AppDynamicsTier>> getAllTiers(@QueryParam("accountId") String accountId,
      @QueryParam("appDynamicsAppId") Long appDynamicsAppId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    return new RestResponse<>(
        appdynamicsService.getTiers(appDynamicsConnectorDTO, orgIdentifier, projectIdentifier, appDynamicsAppId));
  }
}
