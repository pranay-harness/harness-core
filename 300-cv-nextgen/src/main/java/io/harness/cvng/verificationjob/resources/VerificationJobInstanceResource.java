/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.verificationjob.resources;

import io.harness.cvng.verificationjob.beans.TestVerificationBaselineExecutionDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("verification-job-instance")
@Path("verification-job-instance")
@Produces("application/json")
@NextGenManagerAuth
public class VerificationJobInstanceResource {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @GET
  @Timed
  @ExceptionMetered
  @Path("/baseline-executions")
  @ApiOperation(value = "list of last 5 successful baseline executions", nickname = "listBaselineExecutions")
  public RestResponse<List<TestVerificationBaselineExecutionDTO>> baselineExecutions(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("verificationJobIdentifier") String verificationJobIdentifier) {
    return new RestResponse<>(verificationJobInstanceService.getTestJobBaselineExecutions(
        accountId, orgIdentifier, projectIdentifier, verificationJobIdentifier));
  }
}
