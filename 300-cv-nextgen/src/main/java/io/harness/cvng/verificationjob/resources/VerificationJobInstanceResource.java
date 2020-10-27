package io.harness.cvng.verificationjob.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.verificationjob.beans.TestVerificationBaselineExecutionDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.ApiOperation;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

public class VerificationJobInstanceResource {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @GET
  @Timed
  @ExceptionMetered
  @Path("/baseline-executions")
  @ApiOperation(value = "list of last 5 successful baseline executions", nickname = "listBaselineExecutions")
  public RestResponse<List<TestVerificationBaselineExecutionDTO>> baselineExecutions(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("serviceIdentifier") String serviceIdentifier) {
    return new RestResponse<>(verificationJobInstanceService.getTestJobBaselineExecutions(
        accountId, projectIdentifier, orgIdentifier, serviceIdentifier));
  }
}
