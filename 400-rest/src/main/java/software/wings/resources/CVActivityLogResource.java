/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import io.harness.rest.RestResponse;

import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.CVActivityLog;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(VerificationConstants.CV_ACTIVITY_LOGS_PATH)
@Path(VerificationConstants.CV_ACTIVITY_LOGS_PATH)
@Produces("application/json")
@Scope(ResourceType.SERVICE)
public class CVActivityLogResource {
  @Inject private CVActivityLogService cvActivityLogService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject WorkflowService workflowService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVActivityLog>> getActivityLogs(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("stateExecutionId") final String stateExecutionId,
      @QueryParam("cvConfigId") @Valid final String cvConfigId, @QueryParam("startTime") long startTime,
      @QueryParam("endTime") long endTime) {
    // keeping external API same as other apis but we need to fix this. API has to add one minute to startTime to get
    // the data for the heatmap.
    return new RestResponse<>(cvActivityLogService.getActivityLogs(accountId, stateExecutionId, cvConfigId,
        TimeUnit.MILLISECONDS.toMinutes(startTime), TimeUnit.MILLISECONDS.toMinutes(endTime)));
  }
}
