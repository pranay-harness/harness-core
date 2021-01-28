package software.wings.resources.cvng;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.client.CVNGService;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cvng-state")
@Path("/cvng-state")
@Produces("application/json")
@Scope(SETTING)
public class CVNGStateResource {
  @Inject private CVNGService cvngService;

  @GET
  @Path("/verification-jobs")
  @Timed
  @ExceptionMetered
  public RestResponse<VerificationJobDTO> getVerificationJobForUrl(
      @QueryParam("accountId") String accountId, @QueryParam("verificationJobUrl") @NotNull String verificationJobUrl) {
    return new RestResponse<>(cvngService.getVerificationJobs(accountId, verificationJobUrl));
  }
}