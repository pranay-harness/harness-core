/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.resources;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGResourceFilterConstants.PAGE_KEY;
import static io.harness.NGResourceFilterConstants.SIZE_KEY;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.beans.MonitoringSourceDTO;
import io.harness.cvng.core.beans.MonitoringSourceImportStatus;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.dashboard.beans.EnvToServicesDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("ds-config")
@Path("ds-config")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class DSConfigResource {
  @Inject private DSConfigService dsConfigService;
  @Inject private CVConfigService cvConfigService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets list of data source configs", nickname = "getDataSourceConfigs")
  public RestResponse<List<DSConfig>> getDataSourceCVConfigs(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier, @QueryParam("productName") String productName) {
    return new RestResponse<>(dsConfigService.list(accountId, connectorIdentifier, productName));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "creates a data source config", nickname = "createDataSource")
  public void create(@QueryParam("accountId") @Valid final String accountId, @Body DSConfig dsConfig) {
    dsConfigService.create(dsConfig);
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "creates a data source config", nickname = "createDataSourceCVConfig")
  public void update(@NotNull @PathParam("identifier") String identifier,
      @QueryParam("accountId") @Valid final String accountId, @Body DSConfig dsConfig) {
    dsConfigService.update(identifier, dsConfig);
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "deletes all data source configs for a group", nickname = "deleteDataSourceCVConfigByGroup")
  public void deleteByGroup(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("monitoringSourceIdentifier") String monitoringSourceIdentifier) {
    dsConfigService.delete(accountId, orgIdentifier, projectIdentifier, monitoringSourceIdentifier);
  }

  @GET
  @Path("/env-to-services")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets list of env to services mapping for which data sources are configured",
      nickname = "getEnvToServicesList")
  public RestResponse<List<EnvToServicesDTO>>
  getEnvToServicesList(@QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier) {
    return new RestResponse<>(cvConfigService.getEnvToServicesList(accountId, orgIdentifier, projectIdentifier));
  }

  @GET
  @Path("/listMonitoringSources")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets list of monitoring sources", nickname = "listMonitoringSources")
  public ResponseDTO<PageResponse<MonitoringSourceDTO>> listMonitoringSources(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier, @QueryParam(PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(SIZE_KEY) @DefaultValue("100") int size, @QueryParam("filter") String filter) {
    return ResponseDTO.newResponse(
        dsConfigService.listMonitoringSources(accountId, orgIdentifier, projectIdentifier, size, page, filter));
  }

  @GET
  @Path("/getMonitoringStatus")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets the import status of monitoring sources", nickname = "getMonitoringSourceStatus")
  public ResponseDTO<MonitoringSourceImportStatus> getMonitoringSourceImportStatus(
      @NotNull @QueryParam("accountId") final String accountId, @NotNull @QueryParam(ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam("identifier") String identifier) {
    return ResponseDTO.newResponse(
        dsConfigService.getMonitoringSourceImportStatus(accountId, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Path("{identifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a monitoring sources", nickname = "getMonitoringSource")
  public RestResponse<DSConfig> getMonitoringSource(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @QueryParam("accountId") @Valid final String accountId, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    return new RestResponse<>(
        dsConfigService.getMonitoringSource(accountId, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Path("/available-monitoring-sources")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a list of available monitoring sources", nickname = "getAvailableMonitoringSources")
  public RestResponse<List<String>> getAvailableMonitoringSources(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    return new RestResponse<>(
        dsConfigService.getAvailableMonitoringSources(accountId, orgIdentifier, projectIdentifier));
  }
}
