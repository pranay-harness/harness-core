/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.app.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.app.beans.entities.BuildActiveInfo;
import io.harness.app.beans.entities.BuildFailureInfo;
import io.harness.app.beans.entities.CIUsageResult;
import io.harness.app.beans.entities.DashboardBuildExecutionInfo;
import io.harness.app.beans.entities.DashboardBuildRepositoryInfo;
import io.harness.app.beans.entities.DashboardBuildsActiveAndFailedInfo;
import io.harness.app.beans.entities.DashboardBuildsHealthInfo;
import io.harness.core.ci.services.CIOverviewDashboardService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CI)
@Api("ci")
@Path("/ci")
@NextGenManagerAuth
@Produces({"application/json"})
@Consumes({"application/json"})
@Tag(name = "Builds Overview", description = "Contains APIs related to builds overview.")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CIDashboardOverviewResource {
  private final CIOverviewDashboardService ciOverviewDashboardService;
  private final String PROJECT_RESOURCE_TYPE = "PROJECT";
  private final String VIEW_PROJECT_PERMISSION = "core_project_view";
  private final long HR_IN_MS = 60 * 60 * 1000;
  private final long DAY_IN_MS = 24 * HR_IN_MS;

  @GET
  @Path("/buildHealth")
  @ApiOperation(value = "Get build health", nickname = "getBuildHealth")
  @NGAccessControlCheck(resourceType = PROJECT_RESOURCE_TYPE, permission = VIEW_PROJECT_PERMISSION)
  @Operation(operationId = "getBuildHealth",
      summary = "Gets the Build Health by accountIdentifier, orgIdentifier and projectIdentifier for an interval.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns build health with success and failure rate.")
      })
  public ResponseDTO<DashboardBuildsHealthInfo>
  getBuildHealth(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    log.info("Getting build health");
    long previousInterval = startInterval - (endInterval - startInterval + DAY_IN_MS);

    return ResponseDTO.newResponse(ciOverviewDashboardService.getDashBoardBuildHealthInfoWithRate(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousInterval));
  }

  @GET
  @Path("/buildExecution")
  @Operation(operationId = "getBuildExecution",
      summary =
          "Gets the list of Build Execution count by accountIdentifier, orgIdentifier and projectIdentifier for an interval.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the list of build Execution count per day.")
      })
  @ApiOperation(value = "Get build execution", nickname = "getBuildExecution")
  @NGAccessControlCheck(resourceType = PROJECT_RESOURCE_TYPE, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<DashboardBuildExecutionInfo>
  getBuildExecution(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    log.info("Getting build execution");
    return ResponseDTO.newResponse(ciOverviewDashboardService.getBuildExecutionBetweenIntervals(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval));
  }

  @GET
  @Path("/repositoryBuild")
  @Operation(operationId = "getRepositoryBuild",
      summary =
          "Gets the list of Build Execution health of each repository by accountIdentifier, orgIdentifier and projectIdentifier for an interval.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns the list of build Execution count and health of each repository for an interval.")
      })
  @ApiOperation(value = "Get build getRepositoryBuild", nickname = "getRepositoryBuild")
  @NGAccessControlCheck(resourceType = PROJECT_RESOURCE_TYPE, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<DashboardBuildRepositoryInfo>
  getRepositoryBuild(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    log.info("Getting build repository");
    long previousInterval = startInterval - (endInterval - startInterval + DAY_IN_MS);
    return ResponseDTO.newResponse(ciOverviewDashboardService.getDashboardBuildRepository(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousInterval));
  }

  @GET
  @Path("/getBuilds")
  @Operation(operationId = "getActiveAndFailedBuild",
      summary =
          "Gets the list of Active and Failed Builds in specified days by accountIdentifier, orgIdentifier and projectIdentifier.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns the list of build Execution count and health of each repository for an interval.")
      })
  @ApiOperation(value = "Get builds", nickname = "getBuilds")
  @NGAccessControlCheck(resourceType = PROJECT_RESOURCE_TYPE, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<DashboardBuildsActiveAndFailedInfo>
  getActiveAndFailedBuild(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @Parameter(description = "Retrieves build status for the specified number of days. The default value is 20 days.")
      @QueryParam("top") @DefaultValue("20") long days) {
    log.info("Getting builds details failed and active");
    List<BuildFailureInfo> failureInfos = ciOverviewDashboardService.getDashboardBuildFailureInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, days);
    List<BuildActiveInfo> activeInfos = ciOverviewDashboardService.getDashboardBuildActiveInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, days);

    return ResponseDTO.newResponse(
        DashboardBuildsActiveAndFailedInfo.builder().failed(failureInfos).active(activeInfos).build());
  }

  @GET
  @Path("/usage/ci")
  @ApiOperation(value = "Get usage", nickname = "getUsage")
  @NGAccessControlCheck(resourceType = "ACCOUNT", permission = "core_account_view")
  public ResponseDTO<CIUsageResult> getCIUsageData(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.TIMESTAMP) long timestamp) {
    log.info("Getting usage data");

    return ResponseDTO.newResponse(ciOverviewDashboardService.getCIUsageResult(accountIdentifier, timestamp));
  }
}
