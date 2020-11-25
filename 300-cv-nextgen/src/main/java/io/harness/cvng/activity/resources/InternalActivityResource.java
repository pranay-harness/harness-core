package io.harness.cvng.activity.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.INTERNAL_ACTIVITY_RESOURCE;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api(INTERNAL_ACTIVITY_RESOURCE)
@Path(INTERNAL_ACTIVITY_RESOURCE)
@Produces("application/json")
@ExposeInternalException
@LearningEngineAuth
public class InternalActivityResource {
  @Inject private ActivityService activityService;

  @POST
  @Timed
  @ExceptionMetered
  @PublicApi
  @ApiOperation(value = "registers CD 1.0 activity (Internal API) ", nickname = "registerInternalActivity")
  public RestResponse<String> registerActivity(
      @NotNull @QueryParam("accountId") @Valid final String accountId, @Body ActivityDTO activityDTO) {
    return new RestResponse<>(activityService.register(accountId, activityDTO));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("activity-status")
  @PublicApi
  @ApiOperation(value = "get status of activity", nickname = "getActivityStatus")
  public RestResponse<ActivityStatusDTO> getActivityStatus(
      @NotNull @QueryParam("accountId") @Valid final String accountId, @QueryParam("activityId") String activityId) {
    return new RestResponse<>(activityService.getActivityStatus(accountId, activityId));
  }
}
