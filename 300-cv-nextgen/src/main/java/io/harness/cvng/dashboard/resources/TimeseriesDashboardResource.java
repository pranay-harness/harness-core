package io.harness.cvng.dashboard.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVMonitoringCategory;
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
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size) {
    return new RestResponse<>(timeSeriesDashboardService.getSortedAnomalousMetricData(accountId, projectIdentifier,
        orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory != null ? CVMonitoringCategory.valueOf(monitoringCategory) : null, startTimeMillis,
        endTimeMillis, page, size));
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
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size) {
    return new RestResponse<>(timeSeriesDashboardService.getSortedMetricData(accountId, projectIdentifier,
        orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory != null ? CVMonitoringCategory.valueOf(monitoringCategory) : null, startTimeMillis,
        endTimeMillis, page, size));
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
}
