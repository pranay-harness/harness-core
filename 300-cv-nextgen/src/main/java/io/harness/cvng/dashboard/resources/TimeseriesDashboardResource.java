package io.harness.cvng.dashboard.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.beans.NGPageResponse;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("timeseries-dashboard")
@Path("/timeseries-dashboard")
@Produces("application/json")
@ExposeInternalException
public class TimeseriesDashboardResource {
  @Inject TimeSeriesDashboardService timeSeriesDashboardService;

  @GET
  @Path("anomalous-metric-data")
  @Timed
  @ExceptionMetered
  public RestResponse<NGPageResponse<TimeSeriesMetricDataDTO>> getAnomalousMetricData(
      @NotNull @QueryParam("accountId") String accountId,
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
  public RestResponse<NGPageResponse<TimeSeriesMetricDataDTO>> getMetricData(
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
}
