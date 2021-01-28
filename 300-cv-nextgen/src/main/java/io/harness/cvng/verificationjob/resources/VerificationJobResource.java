package io.harness.cvng.verificationjob.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("verification-job")
@Path("verification-job")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class VerificationJobResource {
  @Inject private VerificationJobService verificationJobService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets the verification job for an identifier", nickname = "getVerificationJob")
  public RestResponse<VerificationJobDTO> get(@QueryParam("accountId") @Valid final String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier, @QueryParam("identifier") String identifier) {
    return new RestResponse<>(
        verificationJobService.getVerificationJobDTO(accountId, orgIdentifier, projectIdentifier, identifier));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "upserts a verification job for an identifier", nickname = "upsertVerificationJob")
  public void upsert(
      @QueryParam("accountId") @Valid final String accountId, @Body VerificationJobDTO verificationJobDTO) {
    verificationJobService.upsert(accountId, verificationJobDTO);
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "deletes a verification job for an identifier", nickname = "deleteVerificationJob")
  public void delete(@QueryParam("accountId") @Valid final String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier, @QueryParam("identifier") String identifier) {
    verificationJobService.delete(accountId, orgIdentifier, projectIdentifier, identifier);
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/list")
  @ApiOperation(value = "lists all verification jobs for an identifier", nickname = "listVerificationJobs")
  public RestResponse<PageResponse<VerificationJobDTO>> list(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("projectIdentifier") String projectIdentifier, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("offset") @NotNull Integer offset, @QueryParam("pageSize") @NotNull Integer pageSize,
      @QueryParam("filter") String filter) {
    return new RestResponse<>(
        verificationJobService.list(accountId, projectIdentifier, orgIdentifier, offset, pageSize, filter));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/default-health-job")
  @ApiOperation(
      value = "gets the default health verification job for a project", nickname = "getDefaultHealthVerificationJob")
  public RestResponse<VerificationJobDTO>
  getDefaultHealthVerificationJob(@QueryParam("accountId") @NotNull @Valid final String accountId,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier) {
    return new RestResponse<>(
        verificationJobService.getDefaultHealthVerificationJobDTO(accountId, orgIdentifier, projectIdentifier));
  }
}
