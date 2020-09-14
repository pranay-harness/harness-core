package io.harness.cvng.verificationjob.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("verification-job-instance")
@Path("verification-job-instance")
@Produces("application/json")
@ExposeInternalException
public class VerificationJobInstanceResource {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @POST
  @Timed
  @ExceptionMetered
  public void create(@QueryParam("accountId") @Valid final String accountId,
      @Body VerificationJobInstanceDTO verificationJobInstanceDTO) {
    // TODO: we will need separate token based auth mechanism for this so that the third party systems can call us.
    // TODO: This is just a placeholder. Need to design this API better.
    verificationJobInstanceService.create(accountId, verificationJobInstanceDTO);
  }
}
