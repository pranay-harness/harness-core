package io.harness.cvng.activity.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.ACTIVITY_RESOURCE;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.beans.ActivityDashboardDTO;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivitySummaryDTO;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api(ACTIVITY_RESOURCE)
@Path(ACTIVITY_RESOURCE)
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class ActivityResource {
  @Inject private ActivityService activityService;

  @POST
  @Timed
  @ExceptionMetered
  @PublicApi
  @Path("{webHookToken}")
  @ApiOperation(value = "registers an activity", nickname = "registerActivity")
  public RestResponse<String> registerActivity(@NotNull @PathParam("webHookToken") String webHookToken,
      @NotNull @QueryParam("accountId") @Valid final String accountId, @Body ActivityDTO activityDTO) {
    return new RestResponse<>(activityService.register(accountId, webHookToken, activityDTO));
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
  @Path("/{activityId}/deployment-activity-summary")
  @ApiOperation(value = "get summary of deployment activity", nickname = "getDeploymentActivitySummary")
  public RestResponse<DeploymentActivitySummaryDTO> getDeploymentSummary(
      @NotNull @QueryParam("accountId") String accountId, @NotNull @PathParam("activityId") String activityId) {
    return new RestResponse(activityService.getDeploymentSummary(activityId));
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

  @GET
  @Path("list")
  @ApiOperation(value = "list all activities between a given time range for an environment, project, org",
      nickname = "listActivitiesForDashboard")
  public RestResponse<List<ActivityDashboardDTO>>
  listActivitiesForDashboard(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier, @NotNull @QueryParam("startTime") Long startTime,
      @NotNull @QueryParam("endTime") Long endTime) {
    return new RestResponse(activityService.listActivitiesInTimeRange(accountId, orgIdentifier, projectIdentifier,
        environmentIdentifier, serviceIdentifier, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)));
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
        activityService.getRecentActivityVerificationResults(accountId, orgIdentifier, projectIdentifier, size));
  }

  @GET
  @Path("/{activityId}/activity-risks")
  @ApiOperation(value = "get activity verification result", nickname = "getActivityVerificationResult")
  public RestResponse<ActivityVerificationResultDTO> getActivityVerificationResult(
      @NotNull @QueryParam("accountId") String accountId, @NotNull @PathParam("activityId") String activityId) {
    return new RestResponse(activityService.getActivityVerificationResult(accountId, activityId));
  }

  @GET
  @Path("/{activityId}/deployment-timeseries-data")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get metrics for given activity", nickname = "getDeploymentMetrics")
  public RestResponse<TransactionMetricInfoSummaryPageDTO> getMetrics(
      @NotNull @PathParam("activityId") String activityId, @NotNull @QueryParam("accountId") String accountId,
      @DefaultValue("false") @QueryParam("anomalousMetricsOnly") boolean anomalousMetricsOnly,
      @QueryParam("hostName") String hostName, @QueryParam("filter") String filter,
      @QueryParam("pageNumber") @DefaultValue("0") int pageNumber,
      @QueryParam("pageSize") @DefaultValue("10") int pageSize) {
    return new RestResponse(activityService.getDeploymentActivityTimeSeriesData(
        accountId, activityId, anomalousMetricsOnly, hostName, filter, pageNumber, pageSize));
  }

  @GET
  @Path("/{activityId}/datasource-types")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get datasource types for an activity", nickname = "getDatasourceTypes")
  public RestResponse<Set<DatasourceTypeDTO>> getDatasourceTypes(
      @NotNull @PathParam("activityId") String activityId, @NotNull @QueryParam("accountId") String accountId) {
    return new RestResponse(activityService.getDataSourcetypes(accountId, activityId));
  }

  @GET
  @Path("/{activityId}/clusters")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get logs for given activity", nickname = "getDeploymentLogAnalysisClusters")
  public RestResponse<List<LogAnalysisClusterChartDTO>> getDeploymentLogAnalysisClusters(
      @PathParam("activityId") String activityId, @NotNull @QueryParam("accountId") String accountId,
      @QueryParam("hostName") String hostName) {
    return new RestResponse(activityService.getDeploymentActivityLogAnalysisClusters(accountId, activityId, hostName));
  }

  @Path("/{activityId}/deployment-log-analysis-data")
  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get logs for given activity", nickname = "getDeploymentLogAnalysisResult")
  public RestResponse<PageResponse<LogAnalysisClusterDTO>> getDeploymentLogAnalysisResult(
      @PathParam("activityId") String activityId, @NotNull @QueryParam("accountId") String accountId,
      @QueryParam("label") Integer label, @NotNull @QueryParam("pageNumber") int pageNumber,
      @NotNull @QueryParam("pageSize") int pageSize, @QueryParam("hostName") String hostName) {
    return new RestResponse(activityService.getDeploymentActivityLogAnalysisResult(
        accountId, activityId, label, pageNumber, pageSize, hostName));
  }
}
