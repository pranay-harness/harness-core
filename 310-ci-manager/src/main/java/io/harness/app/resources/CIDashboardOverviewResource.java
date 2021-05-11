package io.harness.app.resources;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.app.beans.entities.BuildActiveInfo;
import io.harness.app.beans.entities.BuildFailureInfo;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CIDashboardOverviewResource {
  private final CIOverviewDashboardService ciOverviewDashboardService;
  @GET
  @Path("/buildHealth")
  @ApiOperation(value = "Get build health", nickname = "getBuildHealth")
  public ResponseDTO<DashboardBuildsHealthInfo> getBuildHealth(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "startInterval") String startInterval,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "endInterval") String endInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    long interval = ChronoUnit.DAYS.between(startDate, endDate);

    if (interval < 0) {
      interval = interval * (-1);
    }

    LocalDate previousStartDate = startDate.minusDays(interval);

    return ResponseDTO.newResponse(ciOverviewDashboardService.getDashBoardBuildHealthInfoWithRate(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartDate.toString()));
  }

  @GET
  @Path("/buildExecution")
  @ApiOperation(value = "Get build execution", nickname = "getBuildExecution")
  public ResponseDTO<DashboardBuildExecutionInfo> getBuildExecution(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "startInterval") String startInterval,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "endInterval") String endInterval) {
    return ResponseDTO.newResponse(ciOverviewDashboardService.getBuildExecutionBetweenIntervals(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval));
  }

  @GET
  @Path("/repositoryBuild")
  @ApiOperation(value = "Get build getRepositoryBuild", nickname = "getRepositoryBuild")
  public ResponseDTO<DashboardBuildRepositoryInfo> getRepositoryBuild(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "startInterval") String startInterval,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "endInterval") String endInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    long interval = ChronoUnit.DAYS.between(startDate, endDate);

    if (interval < 0) {
      interval = interval * (-1);
    }

    LocalDate previousStartDate = startDate.minusDays(interval);
    return ResponseDTO.newResponse(ciOverviewDashboardService.getDashboardBuildRepository(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartDate.toString()));
  }

  @GET
  @Path("/getBuilds")
  @ApiOperation(value = "Get builds", nickname = "getBuilds")
  public ResponseDTO<DashboardBuildsActiveAndFailedInfo> getActiveAndFailedBuild(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("top") @DefaultValue("20") long days) {
    List<BuildFailureInfo> failureInfos = ciOverviewDashboardService.getDashboardBuildFailureInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, days);
    List<BuildActiveInfo> activeInfos = ciOverviewDashboardService.getDashboardBuildActiveInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, days);

    return ResponseDTO.newResponse(
        DashboardBuildsActiveAndFailedInfo.builder().failed(failureInfos).active(activeInfos).build());
  }
}
