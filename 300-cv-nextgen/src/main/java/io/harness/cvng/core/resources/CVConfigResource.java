/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("cv-config")
@Path("cv-config")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class CVConfigResource {
  @Inject private CVConfigService cvConfigService;
  @GET
  @Path("{cvConfigId}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a datasource config", nickname = "getCVConfig")
  public RestResponse<CVConfig> getCVConfig(
      @QueryParam("accountId") @Valid final String accountId, @PathParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(cvConfigService.get(cvConfigId));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves a datasource config", nickname = "saveCVConfig")
  public RestResponse<CVConfig> saveCVConfig(
      @QueryParam("accountId") @Valid final String accountId, @Body CVConfig cvConfig) {
    return new RestResponse<>(cvConfigService.save(cvConfig));
  }

  @POST
  @Path("batch")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves a list of datasource config", nickname = "saveCVConfigs")
  public RestResponse<List<CVConfig>> saveCVConfigs(
      @QueryParam("accountId") @Valid final String accountId, @Body List<CVConfig> cvConfigs) {
    return new RestResponse<>(cvConfigService.save(cvConfigs));
  }

  @PUT
  @Path("{cvConfigId}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "updates a datasource config", nickname = "updateCVConfig")
  public RestResponse<CVConfig> updateCVConfig(@PathParam("cvConfigId") String cvConfigId,
      @QueryParam("accountId") @Valid final String accountId, @Body CVConfig cvConfig) {
    cvConfig.setUuid(cvConfigId);
    cvConfigService.update(cvConfig);
    return new RestResponse<>(cvConfig);
  }

  @PUT
  @Path("batch")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "updates a list of datasource configs", nickname = "updateCVConfigs")
  public RestResponse<List<CVConfig>> updateCVConfigs(
      @QueryParam("accountId") @Valid final String accountId, @Body List<CVConfig> cvConfigs) {
    cvConfigService.update(cvConfigs);
    return new RestResponse<>(cvConfigs);
  }

  @DELETE
  @Path("{cvConfigId}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "deletes a datasource config", nickname = "deleteCVConfig")
  public RestResponse<Void> deleteCVConfig(
      @PathParam("cvConfigId") String cvConfigId, @QueryParam("accountId") @Valid final String accountId) {
    cvConfigService.delete(cvConfigId);
    return null;
  }

  @GET
  @Path("/list")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a list of datasource configs", nickname = "listCVConfigs")
  public RestResponse<List<CVConfig>> listCVConfigs(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier) {
    // keeping it simple for now. We will improve and evolve it based on more requirement on list api.
    return new RestResponse<>(cvConfigService.list(accountId, connectorIdentifier));
  }

  @GET
  @Path("/product-names")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a list of supported products", nickname = "getProductNames")
  public RestResponse<List<String>> getProductNames(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier) {
    return new RestResponse<>(cvConfigService.getProductNames(accountId, connectorIdentifier));
  }

  @GET
  @Path("/datasource-types")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a list of datasource types for this filter", nickname = "getDataSourcetypes")
  public RestResponse<Set<DatasourceTypeDTO>> getDataSourcetypes(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("monitoringCategory") CVMonitoringCategory monitoringCategory) {
    return new RestResponse<>(cvConfigService.getDataSourcetypes(
        accountId, projectIdentifier, orgIdentifier, environmentIdentifier, serviceIdentifier, monitoringCategory));
  }
}
