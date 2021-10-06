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
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterType;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentLogAnalysisFilter;
import io.harness.cvng.core.beans.params.filterParams.DeploymentTimeSeriesAnalysisFilter;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api(ACTIVITY_RESOURCE)
@Path(ACTIVITY_RESOURCE)
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class ActivityResource {
  @Inject private ActivityService activityService;

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
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse(activityService.listActivitiesInTimeRange(projectParams, serviceIdentifier,
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
      @NotEmpty @NotNull @PathParam("activityId") String activityId, @NotNull @QueryParam("accountId") String accountId,
      @DefaultValue("false") @QueryParam("anomalousMetricsOnly") boolean anomalousMetricsOnly,
      @QueryParam("hostName") String hostName, @QueryParam("filter") String filter,
      @QueryParam("healthSources") List<String> healthSourceIdentifiers,
      @QueryParam("pageNumber") @DefaultValue("0") int pageNumber,
      @QueryParam("pageSize") @DefaultValue("10") int pageSize) {
    PageParams pageParams = PageParams.builder().page(pageNumber).size(pageSize).build();
    DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter =
        DeploymentTimeSeriesAnalysisFilter.builder()
            .healthSourceIdentifiers(healthSourceIdentifiers)
            .filter(filter)
            .anomalous(anomalousMetricsOnly)
            .hostName(hostName)
            .build();

    return new RestResponse(activityService.getDeploymentActivityTimeSeriesData(
        accountId, activityId, deploymentTimeSeriesAnalysisFilter, pageParams));
  }

  @GET
  @Path("/{activityId}/datasource-types")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get datasource types for an activity", nickname = "getDatasourceTypes")
  public RestResponse<Set<DatasourceTypeDTO>> getDatasourceTypes(
      @NotNull @NotEmpty @PathParam("activityId") String activityId,
      @NotNull @QueryParam("accountId") String accountId) {
    return new RestResponse(activityService.getDataSourcetypes(accountId, activityId));
  }

  @GET
  @Path("/{activityId}/healthSources")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get health sources  for an activity", nickname = "getHealthSources")
  public RestResponse<Set<HealthSourceDTO>> getHealthSources(
      @NotNull @NotEmpty @PathParam("activityId") String activityId,
      @NotNull @QueryParam("accountId") String accountId) {
    return new RestResponse(activityService.healthSources(accountId, activityId));
  }

  @GET
  @Path("/{activityId}/clusters")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get logs for given activity", nickname = "getDeploymentLogAnalysisClusters")
  public RestResponse<List<LogAnalysisClusterChartDTO>> getDeploymentLogAnalysisClusters(
      @NotNull @NotEmpty @PathParam("activityId") String activityId, @NotNull @QueryParam("accountId") String accountId,
      @QueryParam("hostName") String hostName, @QueryParam("healthSources") List<String> healthSourceIdentifiers,
      @QueryParam("clusterTypes") List<ClusterType> clusterTypes) {
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(healthSourceIdentifiers)
                                                                  .clusterTypes(clusterTypes)
                                                                  .hostName(hostName)
                                                                  .build();

    return new RestResponse(
        activityService.getDeploymentActivityLogAnalysisClusters(accountId, activityId, deploymentLogAnalysisFilter));
  }

  @Path("/{activityId}/deployment-log-analysis-data")
  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get logs for given activity", nickname = "getDeploymentLogAnalysisResult")
  public RestResponse<PageResponse<LogAnalysisClusterDTO>> getDeploymentLogAnalysisResult(
      @PathParam("activityId") String activityId, @NotNull @QueryParam("accountId") String accountId,
      @QueryParam("label") Integer label, @NotNull @QueryParam("pageNumber") int pageNumber,
      @NotNull @QueryParam("pageSize") int pageSize, @QueryParam("hostName") String hostName,
      @QueryParam("healthSources") List<String> healthSourceIdentifiers,
      @QueryParam("clusterTypes") List<ClusterType> clusterTypes) {
    PageParams pageParams = PageParams.builder().page(pageNumber).size(pageSize).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(healthSourceIdentifiers)
                                                                  .clusterTypes(clusterTypes)
                                                                  .hostName(hostName)
                                                                  .build();

    return new RestResponse(activityService.getDeploymentActivityLogAnalysisResult(
        accountId, activityId, label, deploymentLogAnalysisFilter, pageParams));
  }
}
