/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.alert.resources;

import io.harness.cvng.alert.beans.AlertRuleDTO;
import io.harness.cvng.alert.services.api.AlertRuleService;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("alert")
@Path("alert")
@Produces("application/json")
@NextGenManagerAuth
public class AlertRuleResource {
  @Inject private AlertRuleService alertRuleService;

  @GET
  @Timed
  @ExceptionMetered
  @Path("/list")
  @ApiOperation(value = "get list of alerts", nickname = "retrieveAlert")
  public RestResponse<PageResponse<AlertRuleDTO>> retrieveAlert(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, List<AlertRuleDTO> alertRuleDTOList) {
    return new RestResponse<>(alertRuleService.listAlertRules(
        accountId, orgIdentifier, projectIdentifier, offset, pageSize, alertRuleDTOList));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets the alert rule for an identifier", nickname = "getAlertRule")
  public RestResponse<AlertRuleDTO> get(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("identifier") @NotNull String identifier) {
    return new RestResponse<>(
        alertRuleService.getAlertRuleDTO(accountId, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/activityTypes")
  @ApiOperation(value = "gets activity types", nickname = "getActivityTypes")
  public RestResponse<List<ActivityType>> getActivityTypes(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier) {
    return new RestResponse<>(alertRuleService.getActivityTypes(accountId, orgIdentifier, projectIdentifier));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "create alert", nickname = "createAlert")
  public RestResponse<AlertRuleDTO> createAlert(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @Body AlertRuleDTO alertRuleDTO) {
    return new RestResponse<>(alertRuleService.createAlertRule(alertRuleDTO));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "update alert", nickname = "updateAlert")
  public void updateAlert(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @Body AlertRuleDTO alertRuleDTO) {
    alertRuleService.updateAlertRule(accountId, orgIdentifier, projectIdentifier, alertRuleDTO);
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "deletes alert", nickname = "deleteAlert")
  public void deleteAlert(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @PathParam("identifier") @NotNull String identifier) {
    alertRuleService.deleteAlertRule(accountId, orgIdentifier, projectIdentifier, identifier);
  }
}
