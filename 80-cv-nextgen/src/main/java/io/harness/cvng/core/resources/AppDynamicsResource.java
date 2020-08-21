package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("appdynamics")
@Path("/appdynamics")
@Produces("application/json")
public class AppDynamicsResource {
  @Inject private AppDynamicsService appDynamicsService;
  @POST
  @Path("/metric-data")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Set<AppdynamicsValidationResponse>> getMetricData(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("connectorId") @NotNull String connectorId, @QueryParam("appdAppId") @NotNull long appdAppId,
      @QueryParam("appdTierId") @NotNull long appdTierId, @QueryParam("requestGuid") @NotNull String requestGuid,
      @NotNull @Valid @Body List<MetricPack> metricPacks) {
    return new RestResponse<>(appDynamicsService.getMetricPackData(
        accountId, connectorId, orgIdentifier, projectIdentifier, appdAppId, appdTierId, requestGuid, metricPacks));
  }

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AppDynamicsApplication>> getAllApplications(
      @NotNull @QueryParam("accountId") String accountId, @NotNull @QueryParam("connectorId") final String connectorId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier) {
    return new RestResponse<>(
        appDynamicsService.getApplications(accountId, connectorId, orgIdentifier, projectIdentifier));
  }

  @GET
  @Path("/tiers")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<AppDynamicsTier>> getAllTiers(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("connectorId") final String connectorId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @NotNull @QueryParam("appDynamicsAppId") long appdynamicsAppId) {
    return new RestResponse<>(
        appDynamicsService.getTiers(accountId, connectorId, orgIdentifier, projectIdentifier, appdynamicsAppId));
  }
}
