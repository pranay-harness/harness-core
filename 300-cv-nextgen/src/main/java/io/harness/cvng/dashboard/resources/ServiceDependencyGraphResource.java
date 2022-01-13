/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.dashboard.beans.ServiceDependencyGraphDTO;
import io.harness.cvng.dashboard.services.api.ServiceDependencyGraphService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("service-dependency-graph")
@Path("/service-dependency-graph")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class ServiceDependencyGraphResource {
  @Inject private ServiceDependencyGraphService serviceDependencyGraphService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get service dependency graph", nickname = "getServiceDependencyGraph")
  public RestResponse<ServiceDependencyGraphDTO> getServiceDependencyGraph(
      @QueryParam("accountId") @ApiParam(required = true) @NotNull final String accountId,
      @QueryParam("orgIdentifier") @ApiParam(required = true) @NotNull final String orgIdentifier,
      @QueryParam("projectIdentifier") @ApiParam(required = true) @NotNull final String projectIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("envIdentifier") String envIdentifier, @QueryParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("servicesAtRiskFilter") @ApiParam(
          defaultValue = "false") @NotNull final boolean servicesAtRiskFilter) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    if (environmentIdentifier == null) {
      environmentIdentifier = envIdentifier;
    }
    return new RestResponse<>(serviceDependencyGraphService.getDependencyGraph(
        projectParams, serviceIdentifier, environmentIdentifier, servicesAtRiskFilter));
  }
}
