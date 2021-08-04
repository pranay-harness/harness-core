package io.harness.ng.cdOverview.resource;

import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.PROJECT;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cd.NGServiceConstants;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeAndServiceId;
import io.harness.ng.cdOverview.dto.ActiveServiceInstanceSummary;
import io.harness.ng.cdOverview.dto.DashboardWorkloadDeployment;
import io.harness.ng.cdOverview.dto.EnvBuildIdAndInstanceCountInfoList;
import io.harness.ng.cdOverview.dto.EnvIdCountPair;
import io.harness.ng.cdOverview.dto.ExecutionDeploymentInfo;
import io.harness.ng.cdOverview.dto.HealthDeploymentDashboard;
import io.harness.ng.cdOverview.dto.InstancesByBuildIdList;
import io.harness.ng.cdOverview.dto.ServiceDeploymentInfoDTO;
import io.harness.ng.cdOverview.dto.ServiceDeploymentListInfo;
import io.harness.ng.cdOverview.dto.ServiceDetailsInfoDTO;
import io.harness.ng.cdOverview.dto.TimeValuePairListDTO;
import io.harness.ng.cdOverview.service.CDOverviewDashboardService;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.dashboard.DashboardDeploymentsFilteredByServiceInfo;
import io.harness.ng.core.dashboard.DashboardExecutionStatusInfo;
import io.harness.ng.core.dashboard.ExecutionStatusInfo;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<HealthDeploymentDashboard> getDeploymentHealth(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
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
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<ExecutionDeploymentInfo> getDeploymentExecution(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    log.info("Getting deployment execution");
    startInterval = epochShouldBeOfStartOfDay(startInterval);
    endInterval = epochShouldBeOfStartOfDay(endInterval);

    return ResponseDTO.newResponse(cdOverviewDashboardService.getExecutionDeploymentDashboard(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval));
  }

  @GET
  @Path("/getDeployments")
  @ApiOperation(value = "Get deployments", nickname = "getDeployments")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<DashboardExecutionStatusInfo> getDeployments(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @QueryParam("top") @DefaultValue("20") long days) {
    log.info("Getting deployments for active failed and running status");
    return ResponseDTO.newResponse(cdOverviewDashboardService.getDeploymentActiveFailedRunningInfo(
        accountIdentifier, orgIdentifier, projectIdentifier, days));
  }

  @GET
  @Path("/getWorkloads")
  @ApiOperation(value = "Get workloads", nickname = "getWorkloads")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<DashboardWorkloadDeployment> getWorkloads(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval,
      @QueryParam(NGServiceConstants.ENVIRONMENT_TYPE) EnvironmentType envType) {
    log.info("Getting workloads");
    startInterval = epochShouldBeOfStartOfDay(startInterval);
    endInterval = epochShouldBeOfStartOfDay(endInterval);

    long previousStartInterval = startInterval - (endInterval - startInterval + DAY_IN_MS);

    return ResponseDTO.newResponse(cdOverviewDashboardService.getDashboardWorkloadDeployment(accountIdentifier,
        orgIdentifier, projectIdentifier, startInterval, endInterval, previousStartInterval, envType));
  }

  @GET
  @Path("/serviceDetails")
  @ApiOperation(value = "Get service details list", nickname = "getServiceDetails")
  public ResponseDTO<ServiceDetailsInfoDTO> getServiceDeployments(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endTime) throws Exception {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServiceDetailsList(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime));
  }

  @GET
  @Path("/getServicesGrowthTrend")
  @ApiOperation(value = "Get service growth trend", nickname = "getServicesGrowthTrend")
  public ResponseDTO<TimeValuePairListDTO<Integer>> getServicesGrowthTrend(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.TIME_GROUP_BY_TYPE) TimeGroupType timeGroupType) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getServicesGrowthTrend(
        accountIdentifier, orgIdentifier, projectIdentifier, startInterval, endInterval, timeGroupType));
  }

  @GET
  @Path("/getInstanceCountDetailsByService")
  @ApiOperation(value = "Get active service instance count breakdown by env type",
      nickname = "getActiveServiceInstanceCountBreakdown")
  public ResponseDTO<InstanceCountDetailsByEnvTypeAndServiceId>
  getActiveServiceInstanceCountBreakdown(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) List<String> serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveServiceInstanceCountBreakdown(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @GET
  @Path("/getActiveServiceInstanceSummary")
  @ApiOperation(value = "Get active service instance summary", nickname = "getActiveServiceInstanceSummary")
  public ResponseDTO<ActiveServiceInstanceSummary> getActiveServiceInstanceSummary(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGCommonEntityConstants.TIMESTAMP) long timestampInMs) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveServiceInstanceSummary(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs));
  }

  @GET
  @Path("/getEnvBuildInstanceCountByService")
  @ApiOperation(
      value = "Get list of unique environment and build ids with instance count", nickname = "getEnvBuildInstanceCount")
  public ResponseDTO<EnvBuildIdAndInstanceCountInfoList>
  getEnvBuildInstanceCount(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getEnvBuildInstanceCountByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }

  @GET
  @Path("/getInstancesByServiceEnvAndBuilds")
  @ApiOperation(value = "Get list of buildId and instances", nickname = "getActiveInstancesByServiceIdEnvIdAndBuildIds")
  public ResponseDTO<InstancesByBuildIdList> getActiveInstancesByServiceIdEnvIdAndBuildIds(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGCommonEntityConstants.ENVIRONMENT_KEY) String envId,
      @NotNull @QueryParam(NGCommonEntityConstants.BUILDS_KEY) List<String> buildIds) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getActiveInstancesByServiceIdEnvIdAndBuildIds(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, envId, buildIds));
  }

  @GET
  @Path("/getInstanceGrowthTrend")
  @ApiOperation(value = "Get instance growth trend", nickname = "getInstanceGrowthTrend")
  public ResponseDTO<TimeValuePairListDTO<Integer>> getInstanceGrowthTrend(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getInstanceGrowthTrend(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, startInterval, endInterval));
  }

  @GET
  @Path("/getInstanceCountHistory")
  @ApiOperation(value = "Get instance count history", nickname = "getInstanceCountHistory")
  public ResponseDTO<TimeValuePairListDTO<EnvIdCountPair>> getInstanceCountHistory(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    return ResponseDTO.newResponse(cdOverviewDashboardService.getInstanceCountHistory(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, startInterval, endInterval));
  }

  @GET
  @Path("/getDeploymentsByServiceId")
  @ApiOperation(value = "Get deployments by serviceId", nickname = "getDeploymentsByServiceId")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<DashboardDeploymentsFilteredByServiceInfo> getDeploymentsByServiceId(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.SERVICE_KEY) String serviceId) {
    log.info("Getting deployments filtered by serviceId");
    return ResponseDTO.newResponse(cdOverviewDashboardService.getDeploymentsByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId));
  }
}
