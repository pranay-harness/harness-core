package io.harness.cvng.verificationjob.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("verification-job")
@Path("verification-job")
@Produces("application/json")
public class VerificationJobResource {
  @Inject private VerificationJobService verificationJobService;
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<VerificationJobDTO> get(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("identifier") String identifier) {
    return new RestResponse<>(verificationJobService.get(accountId, identifier));
  }

  @PUT
  @Timed
  @ExceptionMetered
  public void upsert(
      @QueryParam("accountId") @Valid final String accountId, @Body VerificationJobDTO verificationJobDTO) {
    verificationJobService.upsert(accountId, verificationJobDTO);
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public void deleteByGroup(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("identifier") String identifier) {
    verificationJobService.delete(accountId, identifier);
  }
}
