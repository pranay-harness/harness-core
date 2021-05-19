package io.harness.ng.cdOverview.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cd.NGServiceConstants;
import io.harness.cdng.Deployment.DashboardDeploymentActiveFailedRunningInfo;
import io.harness.cdng.Deployment.DashboardWorkloadDeployment;
import io.harness.cdng.Deployment.ExecutionDeploymentInfo;
import io.harness.cdng.Deployment.HealthDeploymentDashboard;
import io.harness.cdng.Deployment.ServiceDeploymentInfoDTO;
import io.harness.cdng.Deployment.ServiceDeploymentListInfo;
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
import javax.validation.constraints.NotNull;
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
  private final long HR_IN_MS = 60 * 60 * 1000;
  private final long DAY_IN_MS = 24 * HR_IN_MS;

  private long epochShouldBeOfStartOfDay(long epoch) {
    return epoch - epoch % DAY_IN_MS;
  }
  @GET
  @Path("/deploymentHealth")
  @ApiOperation(value = "Get deployment health", nickname = "getDeploymentHealth")
  public ResponseDTO<HealthDeploymentDashboard> getDeploymentHealth(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END) long endInterval) {
    log.info("Getting deployment health");
    startInterval = epochShouldBeOfStartOfDay(startInterval);
    endInterval = epochShouldBeOfStartOfDay(endInterval);

    long previousStartInterval = startInterval - (endInterval - startInterval + DAY_IN_MS);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getHealthDeploymentDashboard(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartInterval));
  }
  @GET
  @Path("/serviceDeployments")
  @ApiOperation(value = "Get service deployment", nickname = "getServiceDeployments")
  public ResponseDTO<ServiceDeploymentInfoDTO> getServiceDeployment(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGServiceConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGServiceConstants.END_TIME) long endTime,
      @QueryParam(NGServiceConstants.SERVICE_IDENTIFIER) String serviceIdentifier,
      @QueryParam(NGServiceConstants.BUCKET_SIZE_IN_DAYS) @DefaultValue("1") long bucketSizeInDays) {
    log.info("Getting service deployments between %s and %s", startTime, endTime);
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDeployments(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, serviceIdentifier, bucketSizeInDays));
  }

  @GET
  @Path("/serviceDeploymentsInfo")
  @ApiOperation(value = "Get service deployments info", nickname = "getServiceDeploymentsInfo")
  public ResponseDTO<ServiceDeploymentListInfo> getDeploymentExecutionInfo(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @OrgIdentifier @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @ProjectIdentifier @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGServiceConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGServiceConstants.END_TIME) long endTime,
      @QueryParam(NGServiceConstants.SERVICE_IDENTIFIER) String serviceIdentifier,
      @QueryParam(NGServiceConstants.BUCKET_SIZE_IN_DAYS) @DefaultValue("1") long bucketSizeInDays) throws Exception {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDeploymentsInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, serviceIdentifier, bucketSizeInDays));
  }

  @GET
  @Path("/deploymentExecution")
  @ApiOperation(value = "Get deployment execution", nickname = "getDeploymentExecution")
  public ResponseDTO<ExecutionDeploymentInfo> getDeploymentExecution(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END) long endInterval) {
    log.info("Getting deployment execution");
    startInterval = epochShouldBeOfStartOfDay(startInterval);
    endInterval = epochShouldBeOfStartOfDay(endInterval);

    return ResponseDTO.newResponse(cdOverviewDashboardService.getExecutionDeploymentDashboard(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval));
  }

  @GET
  @Path("/getDeployments")
  @ApiOperation(value = "Get deployments", nickname = "getDeployments")
  public ResponseDTO<DashboardDeploymentActiveFailedRunningInfo> getDeployments(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("top") @DefaultValue("20") long days) {
    log.info("Getting deployments for active failed and running status");
    return ResponseDTO.newResponse(cdOverviewDashboardService.getDeploymentActiveFailedRunningInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, days));
  }

  @GET
  @Path("/getWorkloads")
  @ApiOperation(value = "Get workloads", nickname = "getWorkloads")
  public ResponseDTO<DashboardWorkloadDeployment> getWorkloads(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END) long endInterval) {
    log.info("Getting workloads");
    startInterval = epochShouldBeOfStartOfDay(startInterval);
    endInterval = epochShouldBeOfStartOfDay(endInterval);

    long previousStartInterval = startInterval - (endInterval - startInterval + DAY_IN_MS);

    return ResponseDTO.newResponse(cdOverviewDashboardService.getDashboardWorkloadDeployment(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartInterval));
  }
}
