package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("change-event")
@Path("change-event")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@OwnedBy(HarnessTeam.CV)
public class ChangeEventResource {
  @Inject ChangeEventService changeEventService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("register")
  @ApiOperation(value = "register a ChangeEvent", nickname = "registerChangeEvent")
  public RestResponse<Boolean> register(@ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @NotNull @Valid @Body ChangeEventDTO changeEventDTO) {
    return new RestResponse<>(changeEventService.register(changeEventDTO));
  }
}
