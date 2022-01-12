/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
