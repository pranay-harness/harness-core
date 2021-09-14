/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.resources;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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

@Api("appdynamics")
@Path("/appdynamics")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@OwnedBy(CV)
public class AppDynamicsResource {
  @Inject private AppDynamicsService appDynamicsService;
  @POST
  @Path("/metric-data")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get metric data for given metric packs", nickname = "getAppdynamicsMetricData")
  public ResponseDTO<Set<AppdynamicsValidationResponse>> getMetricData(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("connectorIdentifier") @NotNull String connectorIdentifier,
      @QueryParam("appName") @NotNull String appName, @QueryParam("tierName") @NotNull String tierName,
      @QueryParam("requestGuid") @NotNull String requestGuid, @NotNull @Valid @Body List<MetricPackDTO> metricPacks) {
    return ResponseDTO.newResponse(appDynamicsService.getMetricPackData(
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, appName, tierName, requestGuid, metricPacks));
  }

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all appdynamics applications", nickname = "getAppdynamicsApplications")
  public ResponseDTO<PageResponse<AppDynamicsApplication>> getAllApplications(
      @NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("connectorIdentifier") final String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("filter") String filter) {
    return ResponseDTO.newResponse(appDynamicsService.getApplications(
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, offset, pageSize, filter));
  }

  @GET
  @Path("/tiers")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all appdynamics tiers for an application", nickname = "getAppdynamicsTiers")
  public ResponseDTO<PageResponse<AppDynamicsTier>> getAllTiers(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("connectorIdentifier") final String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @NotNull @QueryParam("appName") String appName, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("filter") String filter) {
    return ResponseDTO.newResponse(appDynamicsService.getTiers(
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, appName, offset, pageSize, filter));
  }
}
