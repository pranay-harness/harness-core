package io.harness.ng.cdOverview.resource;

import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_ACCOUNT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.ACCOUNT;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.DeploymentStatsSummary;
import io.harness.dashboards.EnvCount;
import io.harness.dashboards.GroupBy;
import io.harness.dashboards.ProjectsDashboardInfo;
import io.harness.dashboards.ServicesCount;
import io.harness.dashboards.ServicesDashboardInfo;
import io.harness.dashboards.SortBy;
import io.harness.dashboards.TimeBasedDeploymentInfo;
import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Api("landingDashboards")
@Path("/landingDashboards")
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
public class CDLandingDashboardResource {
  private final long HR_IN_MS = 60 * 60 * 1000;
  private final long DAY_IN_MS = 24 * HR_IN_MS;

  private long epochShouldBeOfStartOfDay(long epoch) {
    return epoch - epoch % DAY_IN_MS;
  }
  @Path("/activeServices")
  @ApiOperation(value = "Get Most Active Services", nickname = "getActiveServices")
  @NGAccessControlCheck(resourceType = ACCOUNT, permission = VIEW_ACCOUNT_PERMISSION)
  public ResponseDTO<ServicesDashboardInfo> getActiveServices(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval,
      @NotNull @QueryParam("sortBy") SortBy sortBy) {
    log.info("Getting most active services by : " + sortBy);
    return ResponseDTO.newResponse(ServicesDashboardInfo.builder().build());
  }
  @GET
  @Path("/topProjects")
  @ApiOperation(value = "Get Top Projects as per Deployments", nickname = "getTopProjects")
  @NGAccessControlCheck(resourceType = ACCOUNT, permission = VIEW_ACCOUNT_PERMISSION)
  public ResponseDTO<ProjectsDashboardInfo> getTopProjects(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) {
    log.info("Getting top projects");
    return ResponseDTO.newResponse(ProjectsDashboardInfo.builder().build());
  }

  @GET
  @Path("/deploymentStatsSummary")
  @ApiOperation(value = "Get deployment stats summary", nickname = "getDeploymentStatsSummary")
  @NGAccessControlCheck(resourceType = ACCOUNT, permission = VIEW_ACCOUNT_PERMISSION)
  public ResponseDTO<DeploymentStatsSummary> getDeploymentStatsSummary(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) throws Exception {
    return ResponseDTO.newResponse(DeploymentStatsSummary.builder().build());
  }

  @GET
  @Path("/timeDeploymentStats")
  @ApiOperation(value = "Get time wise deployment stats", nickname = "getTimeWiseDeploymentInfo")
  @NGAccessControlCheck(resourceType = ACCOUNT, permission = VIEW_ACCOUNT_PERMISSION)
  public ResponseDTO<List<TimeBasedDeploymentInfo>> getTimeWiseDeploymentInfo(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval,
      @NotNull @QueryParam("groupBy") GroupBy groupBy) {
    return ResponseDTO.newResponse(Collections.singletonList(TimeBasedDeploymentInfo.builder().build()));
  }

  @GET
  @Path("/servicesCount")
  @ApiOperation(value = "Get services count", nickname = "getServicesCount")
  @NGAccessControlCheck(resourceType = ACCOUNT, permission = VIEW_ACCOUNT_PERMISSION)
  public ResponseDTO<ServicesCount> getServicesCount(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) throws Exception {
    return ResponseDTO.newResponse(ServicesCount.builder().build());
  }

  @GET
  @Path("/envCount")
  @ApiOperation(value = "Get environments count", nickname = "getEnvCount")
  @NGAccessControlCheck(resourceType = ACCOUNT, permission = VIEW_ACCOUNT_PERMISSION)
  public ResponseDTO<EnvCount> getEnvCount(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @NotNull @QueryParam("orgProjectIdentifiers") List<OrgProjectIdentifier> orgProjectIdentifiers,
      @NotNull @QueryParam(NGResourceFilterConstants.START_TIME) long startInterval,
      @NotNull @QueryParam(NGResourceFilterConstants.END_TIME) long endInterval) throws Exception {
    return ResponseDTO.newResponse(EnvCount.builder().build());
  }
}
