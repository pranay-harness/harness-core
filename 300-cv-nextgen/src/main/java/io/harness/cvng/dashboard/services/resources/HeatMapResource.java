package io.harness.cvng.dashboard.services.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.EnvServiceRiskDTO;
import io.harness.cvng.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("heatmap")
@Path("/heatmap")
@Produces("application/json")
@ExposeInternalException
public class HeatMapResource {
  @Inject private HeatMapService heatMapService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<Map<CVMonitoringCategory, SortedSet<HeatMapDTO>>> getHeatMap(
      @QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier,
      @QueryParam("serviceIdentifier") final String serviceIdentifier,
      @QueryParam("envIdentifier") final String envIdentifier,
      @QueryParam("startTimeMs") @NotNull final Long startTimeMs,
      @QueryParam("endTimeMs") @NotNull final Long endTimeMs) {
    return new RestResponse<>(heatMapService.getHeatMap(accountId, projectIdentifier, serviceIdentifier, envIdentifier,
        Instant.ofEpochMilli(startTimeMs), Instant.ofEpochMilli(endTimeMs)));
  }

  @GET
  @Path("/category-risks")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<CVMonitoringCategory, Integer>> getCategoryRiskMap(
      @QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("orgIdentifier") @NotNull final String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier,
      @QueryParam("serviceIdentifier") final String serviceIdentifier,
      @QueryParam("envIdentifier") final String envIdentifier) {
    return new RestResponse<>(heatMapService.getCategoryRiskScores(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier));
  }

  @GET
  @Path("/env-service-risks")
  @Timed
  @ExceptionMetered
  public RestResponse<List<EnvServiceRiskDTO>> getEnvServiceRisks(
      @QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("orgIdentifier") @NotNull final String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier) {
    return new RestResponse<>(heatMapService.getEnvServiceRiskScores(accountId, orgIdentifier, projectIdentifier));
  }
}
