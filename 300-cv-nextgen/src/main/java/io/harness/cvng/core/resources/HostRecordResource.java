/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.HOST_RECORD_RESOURCE_PATH;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.HostRecordDTO;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api(HOST_RECORD_RESOURCE_PATH)
@Path(HOST_RECORD_RESOURCE_PATH)
@Produces("application/json")
@ExposeInternalException
public class HostRecordResource {
  @Inject private HostRecordService hostRecordService;
  @POST
  @Timed
  @ExceptionMetered
  @DelegateAuth
  @ApiOperation(
      value = "saves hosts collected while doing canary verification", nickname = "saveVerificationHostRecords")
  public RestResponse<Void>
  saveHostRecords(
      @QueryParam("accountId") @NotNull String accountId, @NotNull @Valid @Body List<HostRecordDTO> hostRecordDTOs) {
    hostRecordService.save(hostRecordDTOs);
    return new RestResponse<>(null);
  }
}
