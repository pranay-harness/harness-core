package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.ActivityDTO;
import io.harness.cvng.core.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.core.services.api.ActivityService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("activity")
@Path("activity")
@Produces("application/json")
@PublicApi
// Since it's a public API. This enables better error messages.
@ExposeInternalException
public class ActivityResource {
  @Inject ActivityService activityService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("{webHookToken}")
  public void registerActivity(@PathParam("webHookToken") String webHookToken,
      @QueryParam("accountId") @Valid final String accountId, @Body ActivityDTO activityDTO) {
    activityService.register(accountId, webHookToken, activityDTO);
  }

  @GET
  @Path("recent-deployment-activity-verifications")
  public RestResponse<List<DeploymentActivityVerificationResultDTO>> getRecentDeploymentActivityVerifications(
      @QueryParam("accountId") String accountId, @QueryParam("projectIdentifier") String projectIdentifier) {
    return new RestResponse<>(activityService.getRecentDeploymentActivityVerifications(accountId, projectIdentifier));
  }
}
