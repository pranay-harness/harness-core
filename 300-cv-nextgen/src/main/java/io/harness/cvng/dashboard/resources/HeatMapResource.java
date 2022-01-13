/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.CategoryRisksDTO;
import io.harness.cvng.dashboard.beans.EnvServiceRiskDTO;
import io.harness.cvng.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@NextGenManagerAuth
public class HeatMapResource {
  @Inject private HeatMapService heatMapService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get heatmap for a time range", nickname = "getHeatMap")
  public RestResponse<Map<CVMonitoringCategory, SortedSet<HeatMapDTO>>> getHeatMap(
      @QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier,
      @QueryParam("serviceIdentifier") final String serviceIdentifier,
      @QueryParam("envIdentifier") final String envIdentifier,
      @QueryParam("startTimeMs") @NotNull final Long startTimeMs,
      @QueryParam("endTimeMs") @NotNull final Long endTimeMs) {
    return new RestResponse<>(heatMapService.getHeatMap(accountId, orgIdentifier, projectIdentifier, serviceIdentifier,
        envIdentifier, Instant.ofEpochMilli(startTimeMs), Instant.ofEpochMilli(endTimeMs)));
  }

  @GET
  @Path("/category-risks")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get current risk for all categories", nickname = "getCategoryRiskMap")
  public RestResponse<CategoryRisksDTO> getCategoryRiskMap(@QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("orgIdentifier") @NotNull final String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier,
      @QueryParam("serviceIdentifier") final String serviceIdentifier,
      @QueryParam("envIdentifier") final String envIdentifier) {
    return new RestResponse<>(heatMapService.getCategoryRiskScores(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier));
  }

  @GET
  @Path("/risk-summary-popover")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get current risk summary", nickname = "getRiskSummaryPopover")
  public RestResponse<RiskSummaryPopoverDTO> getRiskSummaryPopover(
      @QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("orgIdentifier") @NotNull final String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier,
      @NotNull @QueryParam("endTime") Long endTime, @QueryParam("serviceIdentifier") final String serviceIdentifier,
      @QueryParam("category") CVMonitoringCategory category) {
    return new RestResponse<>(heatMapService.getRiskSummaryPopover(
        accountId, orgIdentifier, projectIdentifier, Instant.ofEpochMilli(endTime), serviceIdentifier, category));
  }

  @GET
  @Path("/env-service-risks")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get current risks for each env/service combination", nickname = "getEnvServiceRisks")
  public RestResponse<List<EnvServiceRiskDTO>> getEnvServiceRisks(
      @QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("orgIdentifier") @NotNull final String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier) {
    return new RestResponse<>(heatMapService.getEnvServiceRiskScores(accountId, orgIdentifier, projectIdentifier));
  }
}
