package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.core.beans.ActivityDTO;
import io.harness.cvng.core.services.api.ActivityService;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("activity")
@Path("activity")
@Produces("application/json")
@PublicApi
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
}
