package io.harness.cvng.activity.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.ACTIVITY_RESOURCE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.beans.ActivityDashboardDTO;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.KubernetesActivitySourceDTO;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.KubernetesActivityDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import retrofit2.http.Body;

import java.time.Instant;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(ACTIVITY_RESOURCE)
@Path(ACTIVITY_RESOURCE)
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class ActivityResource {
  @Inject private ActivityService activityService;
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;

  @POST
  @Timed
  @ExceptionMetered
  @PublicApi
  @Path("{webHookToken}")
  @ApiOperation(value = "registers an activity", nickname = "registerActivity")
  public void registerActivity(@NotNull @PathParam("webHookToken") String webHookToken,
      @NotNull @QueryParam("accountId") @Valid final String accountId, @Body ActivityDTO activityDTO) {
    activityService.register(accountId, webHookToken, activityDTO);
  }

  @GET
  @Path("recent-deployment-activity-verifications")
  @ApiOperation(
      value = "get recent deployment activity verification", nickname = "getRecentDeploymentActivityVerifications")
  public RestResponse<List<DeploymentActivityVerificationResultDTO>>
  getRecentDeploymentActivityVerifications(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier) {
    return new RestResponse<>(
        activityService.getRecentDeploymentActivityVerifications(accountId, orgIdentifier, projectIdentifier));
  }

  @GET
  @Path("deployment-activity-verifications/{deploymentTag}")
  @ApiOperation(
      value = "get deployment activities for given build tag", nickname = "getDeploymentActivityVerificationsByTag")
  public RestResponse<DeploymentActivityResultDTO>
  getDeploymentActivityVerificationsByTag(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @PathParam("deploymentTag") String deploymentTag) {
    return new RestResponse(activityService.getDeploymentActivityVerificationsByTag(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, deploymentTag));
  }

  @GET
  @Path("deployment-activity-verifications-popover-summary/{deploymentTag}")
  @ApiOperation(value = "get deployment activities summary for given build tag",
      nickname = "getDeploymentActivityVerificationsPopoverSummaryByTag")
  public RestResponse<DeploymentActivityPopoverResultDTO>
  getDeploymentActivityVerificationsPopoverSummary(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @PathParam("deploymentTag") String deploymentTag) {
    return new RestResponse(activityService.getDeploymentActivityVerificationsPopoverSummary(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, deploymentTag));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/kubernetes-source")
  @ApiOperation(value = "register a kubernetes event source", nickname = "registerKubernetesSource")
  public RestResponse<String> registerKubernetesSource(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @Body KubernetesActivitySourceDTO activitySourceDTO) {
    return new RestResponse<>(kubernetesActivitySourceService.saveKubernetesSource(
        accountId, orgIdentifier, projectIdentifier, activitySourceDTO));
  }

  @POST
  @Timed
  @ExceptionMetered
  @DelegateAuth
  @Path("/kubernetes-activities")
  @ApiOperation(value = "saves a list of kubernetes activities", nickname = "saveKubernetesActivities")
  public RestResponse<Boolean> saveKubernetesActivities(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("activitySourceId") @NotNull String activitySourceId,
      @NotNull @Valid @Body List<KubernetesActivityDTO> activities) {
    return new RestResponse<>(
        kubernetesActivitySourceService.saveKubernetesActivities(accountId, activitySourceId, activities));
  }

  @GET
  @Path("list")
  @ApiOperation(value = "list all activities between a given time range for an environment, project, org",
      nickname = "listActivitiesForDashboard")
  public RestResponse<List<ActivityDashboardDTO>>
  listActivitiesForDashboard(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @NotNull @QueryParam("startTime") Long startTime, @NotNull @QueryParam("endTime") Long endTime) {
    return new RestResponse(activityService.listActivitiesInTimeRange(orgIdentifier, projectIdentifier,
        environmentIdentifier, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)));
  }

  @GET
  @Path("recent-activity-verifications")
  @ApiOperation(
      value = "get a list of recent activity verification results", nickname = "getRecentActivityVerificationResults")
  public RestResponse<List<ActivityVerificationResultDTO>>
  getRecentActivityVerificationResults(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier, @QueryParam("size") int size) {
    return new RestResponse(
        activityService.getRecentActivityVerificationResults(orgIdentifier, projectIdentifier, size));
  }

  @GET
  @Path("/{activityId}/activity-risks")
  @ApiOperation(value = "get activity verification result", nickname = "getActivityVerificationResult")
  public RestResponse<ActivityVerificationResultDTO> getActivityVerificationResult(
      @NotNull @QueryParam("accountId") String accountId, @NotNull @PathParam("activityId") String activityId) {
    return new RestResponse(activityService.getActivityVerificationResult(accountId, activityId));
  }
}
