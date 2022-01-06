/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.analysis.beans.LiveMonitoringLogAnalysisClusterDTO;
import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.beans.params.filterParams.LiveMonitoringLogAnalysisFilter;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO;
import io.harness.cvng.dashboard.beans.LogDataByTag;
import io.harness.cvng.dashboard.services.api.LogDashboardService;
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
import java.util.SortedSet;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("log-dashboard")
@Path("/log-dashboard")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class LogDashboardResource {
  @Inject private LogDashboardService logDashboardService;

  @GET
  @Path("/anomalous-logs")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get anomalous logs for a time range", nickname = "getAnomalousLogs")
  public RestResponse<PageResponse<AnalyzedLogDataDTO>> getAnomalousLogs(@QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("monitoringCategory") String monitoringCategory,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size) {
    return new RestResponse<>(
        logDashboardService.getAnomalousLogs(accountId, projectIdentifier, orgIdentifier, serviceIdentifier,
            environmentIdentifier, monitoringCategory != null ? CVMonitoringCategory.valueOf(monitoringCategory) : null,
            startTimeMillis, endTimeMillis, page, size));
  }

  @GET
  @Path("/all-logs")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all logs for a time range", nickname = "getAllLogs")
  public RestResponse<PageResponse<AnalyzedLogDataDTO>> getAllLogs(@QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("monitoringCategory") String monitoringCategory,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size) {
    return new RestResponse<>(
        logDashboardService.getAllLogs(accountId, projectIdentifier, orgIdentifier, serviceIdentifier,
            environmentIdentifier, monitoringCategory != null ? CVMonitoringCategory.valueOf(monitoringCategory) : null,
            startTimeMillis, endTimeMillis, page, size));
  }

  @GET
  @Path("/log-count-by-tags")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get a sorted tag vs logs list", nickname = "getTagCount")
  public RestResponse<SortedSet<LogDataByTag>> getTagCount(@QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("monitoringCategory") String monitoringCategory,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis) {
    return new RestResponse<>(
        logDashboardService.getLogCountByTag(accountId, projectIdentifier, orgIdentifier, serviceIdentifier,
            environmentIdentifier, monitoringCategory != null ? CVMonitoringCategory.valueOf(monitoringCategory) : null,
            startTimeMillis, endTimeMillis));
  }

  @GET
  @Path("/{activityId}/log-count-by-tags")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get a sorted tag vs logs list for an activity", nickname = "getTagCountForActivity")
  public RestResponse<SortedSet<LogDataByTag>> getTagCountForActivity(@QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis,
      @NotNull @PathParam("activityId") String activityId) {
    return new RestResponse<>(logDashboardService.getLogCountByTagForActivity(accountId, projectIdentifier,
        orgIdentifier, activityId, Instant.ofEpochMilli(startTimeMillis), Instant.ofEpochMilli(endTimeMillis)));
  }

  @GET
  @Path("/{activityId}/logs")
  @ApiOperation(value = "get activity logs for given activityId", nickname = "getActivityLogs")
  public RestResponse<PageResponse<AnalyzedLogDataDTO>> getActivityLogs(
      @NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier, @NotNull @QueryParam("startTime") Long startTimeMillis,
      @NotNull @QueryParam("endTime") Long endTimeMillis, @QueryParam("anomalousOnly") boolean anomalousOnly,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size,
      @NotNull @PathParam("activityId") String activityId) {
    return new RestResponse(logDashboardService.getActivityLogs(activityId, accountId, projectIdentifier, orgIdentifier,
        environmentIdentifier, serviceIdentifier, startTimeMillis, endTimeMillis, anomalousOnly, page, size));
  }

  @GET
  @Path("/logs-data")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all log data for a time range", nickname = "getAllLogsData")
  public RestResponse<PageResponse<AnalyzedLogDataDTO>> getAllLogsData(@QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("clusterTypes") List<LogAnalysisTag> clusterTypes,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis,
      @QueryParam("healthSources") List<String> healthSourceIdentifiers,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .build();
    TimeRangeParams timeRangeParams = TimeRangeParams.builder()
                                          .startTime(Instant.ofEpochMilli(startTimeMillis))
                                          .endTime(Instant.ofEpochMilli(endTimeMillis))
                                          .build();
    PageParams pageParams = PageParams.builder().page(page).size(size).build();
    LiveMonitoringLogAnalysisFilter liveMonitoringLogAnalysisFilter =
        LiveMonitoringLogAnalysisFilter.builder()
            .healthSourceIdentifiers(healthSourceIdentifiers)
            .clusterTypes(clusterTypes)
            .build();

    return new RestResponse<>(logDashboardService.getAllLogsData(
        serviceEnvironmentParams, timeRangeParams, liveMonitoringLogAnalysisFilter, pageParams));
  }

  @GET
  @Path("/logs-cluster")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all log cluster data for a time range", nickname = "getAllLogsClusterData")
  public RestResponse<List<LiveMonitoringLogAnalysisClusterDTO>> getLogsClusterData(
      @QueryParam("accountId") String accountId, @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("clusterTypes") List<LogAnalysisTag> clusterTypes,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis,
      @QueryParam("healthSources") List<String> healthSourceIdentifiers) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .build();
    TimeRangeParams timeRangeParams = TimeRangeParams.builder()
                                          .startTime(Instant.ofEpochMilli(startTimeMillis))
                                          .endTime(Instant.ofEpochMilli(endTimeMillis))
                                          .build();
    LiveMonitoringLogAnalysisFilter liveMonitoringLogAnalysisFilter =
        LiveMonitoringLogAnalysisFilter.builder()
            .healthSourceIdentifiers(healthSourceIdentifiers)
            .clusterTypes(clusterTypes)
            .build();

    return new RestResponse<>(logDashboardService.getLogAnalysisClusters(
        serviceEnvironmentParams, timeRangeParams, liveMonitoringLogAnalysisFilter));
  }
}
