/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("onboarding")
@Path("/onboarding")
@Produces("application/json")
@NextGenManagerAuth
@ExposeInternalException
public class OnboardingResource {
  @Inject private OnboardingService onboardingService;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "onboarding api response", nickname = "getOnboardingResponse")
  public RestResponse<OnboardingResponseDTO> getOnboardingResponse(
      @QueryParam("accountId") @NotNull String accountId, OnboardingRequestDTO onboardingRequestDTO) {
    return new RestResponse<>(onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/connector")
  @ApiOperation(value = "connector api response", nickname = "validateConnector")
  public RestResponse<Void> validateConnector(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("connectorIdentifier") @NotNull String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("tracingId") @NotNull String tracingId,
      @QueryParam("dataSourceType") @NotNull DataSourceType dataSourceType) {
    onboardingService.checkConnectivity(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, tracingId, dataSourceType);
    return new RestResponse<>(null);
  }
}
