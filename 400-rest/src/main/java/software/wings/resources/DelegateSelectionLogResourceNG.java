/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.service.intfc.DelegateSelectionLogsService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api(value = "/ng/delegateSelectionLog", hidden = true)
@Path("/ng/delegateSelectionLog")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@Slf4j
public class DelegateSelectionLogResourceNG {
  private DelegateSelectionLogsService delegateSelectionLogsService;

  @Inject
  public DelegateSelectionLogResourceNG(DelegateSelectionLogsService delegateSelectionLogsService) {
    this.delegateSelectionLogsService = delegateSelectionLogsService;
  }

  @GET
  @Path("/delegateInfo")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateSelectionLogParams> getSelectedDelegate(
      @QueryParam("accountId") String accountId, @QueryParam("taskId") String taskId) {
    return new RestResponse(delegateSelectionLogsService.fetchSelectedDelegateForTask(accountId, taskId));
  }
}
