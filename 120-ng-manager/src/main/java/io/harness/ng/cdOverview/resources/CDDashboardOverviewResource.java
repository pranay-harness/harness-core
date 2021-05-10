package io.harness.ng.cdOverview.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.Deployment.DashboardDeploymentActiveFailedRunningInfo;
import io.harness.cdng.Deployment.DashboardWorkloadDeployment;
import io.harness.cdng.Deployment.ExecutionDeploymentDetailInfo;
import io.harness.cdng.Deployment.ExecutionDeploymentInfo;
import io.harness.cdng.Deployment.HealthDeploymentDashboard;
import io.harness.cdng.service.dashboard.CDOverviewDashboardService;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
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

@OwnedBy(HarnessTeam.CDC)
@Api("dashboard")
@Path("/dashboard")
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
public class CDDashboardOverviewResource {
  private final CDOverviewDashboardService cdOverviewDashboardService;
  @GET
  @Path("/deploymentHealth")
  @ApiOperation(value = "Get deployment health", nickname = "getDeploymentHealth")
  public ResponseDTO<HealthDeploymentDashboard> getDeploymentHealth(
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
    return ResponseDTO.newResponse(cdOverviewDashboardService.getHealthDeploymentDashboard(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartDate.toString()));
  }

  @GET
  @Path("/deploymentsInfo")
  @ApiOperation(value = "Get deployments info", nickname = "getDeploymentsInfo")
  public ResponseDTO<ExecutionDeploymentDetailInfo> getDeploymentExecutionInfo(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @OrgIdentifier @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @ProjectIdentifier @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "startInterval") String startInterval,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "endInterval") String endInterval) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getDeploymentsExecutionInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval));
  }

  @GET
  @Path("/deploymentExecution")
  @ApiOperation(value = "Get deployment execution", nickname = "getDeploymentExecution")
  public ResponseDTO<ExecutionDeploymentInfo> getDeploymentExecution(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "startInterval") String startInterval,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "endInterval") String endInterval) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getExecutionDeploymentDashboard(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval));
  }

  @GET
  @Path("/getDeployments")
  @ApiOperation(value = "Get deployments", nickname = "getDeployments")
  public ResponseDTO<DashboardDeploymentActiveFailedRunningInfo> getDeployments(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("top") @DefaultValue("20") long days) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getDeploymentActiveFailedRunningInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, days));
  }

  @GET
  @Path("/getWorkloads")
  @ApiOperation(value = "Get workloads", nickname = "getWorkloads")
  public ResponseDTO<DashboardWorkloadDeployment> getWorkloads(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "startInterval") String startInterval,
      @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "endInterval") String endInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    long interval = ChronoUnit.DAYS.between(startDate, endDate);

    if (interval < 0) {
      interval = interval * (-1);
    }

    LocalDate previousStartDate = startDate.minusDays(interval);

    return ResponseDTO.newResponse(cdOverviewDashboardService.getDashboardWorkloadDeployment(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartDate.toString()));
  }
}
