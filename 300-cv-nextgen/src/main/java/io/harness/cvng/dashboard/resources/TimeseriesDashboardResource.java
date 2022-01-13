/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.beans.params.filterParams.TimeSeriesAnalysisFilter;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
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
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("timeseries-dashboard")
@Path("/timeseries-dashboard")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class TimeseriesDashboardResource {
  @Inject TimeSeriesDashboardService timeSeriesDashboardService;

  @GET
  @Path("anomalous-metric-data")
  @Timed
  @ExceptionMetered
  @ApiOperation(
      value = "get anomalous time series data in a given time range", nickname = "getAnomalousMetricDashboardData")
  public RestResponse<PageResponse<TimeSeriesMetricDataDTO>>
  getAnomalousMetricData(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("monitoringCategory") String monitoringCategory,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis,
      @NotNull @QueryParam("analysisStartTime") Long analysisStartTime, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("10") int size, @QueryParam("filter") String filter,
      @QueryParam("datasourceType") DataSourceType datasourceType) {
    // TODO: Change this to a request body. THis is too many query params.
    return new RestResponse<>(timeSeriesDashboardService.getSortedMetricData(accountId, projectIdentifier,
        orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory != null ? CVMonitoringCategory.valueOf(monitoringCategory) : null, startTimeMillis,
        endTimeMillis, analysisStartTime, true, page, size, filter, datasourceType));
  }

  @GET
  @Path("metric-data")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all time series data in a given time range", nickname = "getMetricData")
  public RestResponse<PageResponse<TimeSeriesMetricDataDTO>> getMetricData(
      @NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("monitoringCategory") String monitoringCategory,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis,
      @NotNull @QueryParam("analysisStartTime") Long analysisStartTime, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("10") int size, @QueryParam("filter") String filter,
      @QueryParam("datasourceType") DataSourceType datasourceType) {
    // TODO: Change this to a request body. This is too many query params.
    return new RestResponse<>(timeSeriesDashboardService.getSortedMetricData(accountId, projectIdentifier,
        orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory != null ? CVMonitoringCategory.valueOf(monitoringCategory) : null, startTimeMillis,
        endTimeMillis, analysisStartTime, false, page, size, filter, datasourceType));
  }

  @GET
  @Path("/{activityId}/metrics")
  @ApiOperation(value = "get activity metrics for given activityId", nickname = "getActivityMetrics")
  public RestResponse<PageResponse<TimeSeriesMetricDataDTO>> getActivityMetrics(
      @NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier, @NotNull @QueryParam("startTime") Long startTimeMillis,
      @NotNull @QueryParam("endTime") Long endTimeMillis, @QueryParam("anomalousOnly") boolean anomalousOnly,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size,
      @NotNull @PathParam("activityId") String activityId) {
    return new RestResponse(
        timeSeriesDashboardService.getActivityMetrics(activityId, accountId, projectIdentifier, orgIdentifier,
            environmentIdentifier, serviceIdentifier, startTimeMillis, endTimeMillis, anomalousOnly, page, size));
  }

  @GET
  @Path("metrics")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all time series data in a given time range", nickname = "getTimeSeriesMetricData")
  public RestResponse<PageResponse<TimeSeriesMetricDataDTO>> getTimeSeriesMetricData(
      @NotNull @QueryParam("accountId") String accountId, @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @NotNull @QueryParam("startTime") Long startTimeMillis, @NotNull @QueryParam("endTime") Long endTimeMillis,
      @QueryParam("anomalous") @DefaultValue("false") boolean anomalous, @QueryParam("filter") String filter,
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
    TimeSeriesAnalysisFilter timeSeriesAnalysisFilter = TimeSeriesAnalysisFilter.builder()
                                                            .filter(filter)
                                                            .anomalous(anomalous)
                                                            .healthSourceIdentifiers(healthSourceIdentifiers)
                                                            .build();

    return new RestResponse<>(timeSeriesDashboardService.getTimeSeriesMetricData(
        serviceEnvironmentParams, timeRangeParams, timeSeriesAnalysisFilter, pageParams));
  }
}
