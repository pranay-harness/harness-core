/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.resources;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.SLORiskCountResponse;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardApiFilter;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.cvng.servicelevelobjective.services.api.SLODashboardService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("slo-dashboard")
@Path("slo-dashboard")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class SLODashboardResource {
  @Inject private SLODashboardService sloDashboardService;

  public static final String SLO = "SLO";
  public static final String VIEW_PERMISSION = "chi_slo_view";

  @GET
  @Path("widgets")
  @ExceptionMetered
  @ApiOperation(value = "get widget list", nickname = "getSLODashboardWidgets")
  @NGAccessControlCheck(resourceType = SLO, permission = VIEW_PERMISSION)
  public ResponseDTO<PageResponse<SLODashboardWidget>> getSloDashboardWidgets(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") @AccountIdentifier String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier,
      @BeanParam SLODashboardApiFilter filter, @BeanParam PageParams pageParams) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(sloDashboardService.getSloDashboardWidgets(projectParams, filter, pageParams));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("risk-count")
  @ApiOperation(
      value = "get all service level objectives count by risk", nickname = "getServiceLevelObjectivesRiskCount")
  @NGAccessControlCheck(resourceType = SLO, permission = VIEW_PERMISSION)
  public ResponseDTO<SLORiskCountResponse>
  getServiceLevelObjectivesRiskCount(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") @AccountIdentifier String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier,
      @BeanParam SLODashboardApiFilter serviceLevelObjectiveFilter) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(sloDashboardService.getRiskCount(projectParams, serviceLevelObjectiveFilter));
  }
}
