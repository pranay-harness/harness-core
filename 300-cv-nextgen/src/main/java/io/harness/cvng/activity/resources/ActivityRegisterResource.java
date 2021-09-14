/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("activity-register")
@Path("activity-register")
@Produces("application/json")
@ExposeInternalException
public class ActivityRegisterResource {
  @Inject private ActivityService activityService;

  @POST
  @Timed
  @ExceptionMetered
  @PublicApi
  @Path("{webHookToken}")
  @ApiOperation(value = "public api to register an activity", nickname = "activityRegistration")
  public void registerActivity(@NotNull @PathParam("webHookToken") String webHookToken,
      @NotNull @QueryParam("accountId") @Valid final String accountId, @Body ActivityDTO activityDTO) {
    activityService.register(accountId, webHookToken, activityDTO);
  }
}
