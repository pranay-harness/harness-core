/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.resources;

import static io.harness.cvng.core.services.CVNextGenConstants.ACTIVITY_RESOURCE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivitySummaryDTO;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterType;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.PageParams;
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
import java.util.Arrays;
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
  @Path("/{activityId}/deployment-activity-summary")
  @ApiOperation(value = "get summary of deployment activity", nickname = "getDeploymentActivitySummary")
  public RestResponse<DeploymentActivitySummaryDTO> getDeploymentSummary(
      @NotNull @QueryParam("accountId") String accountId, @NotNull @PathParam("activityId") String activityId) {
    return new RestResponse(activityService.getDeploymentSummary(activityId));
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
      @QueryParam("hostName") String hostName, @QueryParam("healthSource") List<String> healthSourceIdentifier,
      @QueryParam("healthSources") List<String> healthSourceIdentifiers,
      @QueryParam("clusterType") List<ClusterType> clusterType,
      @QueryParam("clusterTypes") List<ClusterType> clusterTypes) {
    if (isNotEmpty(healthSourceIdentifier)) {
      healthSourceIdentifiers = healthSourceIdentifier;
    }
    if (isNotEmpty(clusterType)) {
      clusterTypes = clusterType;
    }
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
      @QueryParam("healthSource") List<String> healthSourceIdentifier,
      @QueryParam("healthSources") List<String> healthSourceIdentifiers,
      @QueryParam("clusterType") ClusterType clusterType, @QueryParam("clusterTypes") List<ClusterType> clusterTypes) {
    PageParams pageParams = PageParams.builder().page(pageNumber).size(pageSize).build();
    if (clusterType != null) {
      clusterTypes = Arrays.asList(clusterType);
    }
    if (isNotEmpty(healthSourceIdentifier)) {
      healthSourceIdentifiers = healthSourceIdentifier;
    }
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(healthSourceIdentifiers)
                                                                  .clusterTypes(clusterTypes)
                                                                  .hostName(hostName)
                                                                  .build();

    return new RestResponse(activityService.getDeploymentActivityLogAnalysisResult(
        accountId, activityId, label, deploymentLogAnalysisFilter, pageParams));
  }
}
