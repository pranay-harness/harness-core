package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.beans.TimeSeriesTestDataDTO;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/timeseries")
@Path("/timeseries")
@Produces("application/json")
public class TimeSeriesResource {
  @Inject private TimeSeriesService timeSeriesService;

  @GET
  @Path("/metric-template")
  @Timed
  @ExceptionMetered
  public RestResponse<List<TimeSeriesMetricDefinition>> getMetricDefinitions(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("cvConfigId") @NotNull String cvConfigId) {
    return new RestResponse<>(timeSeriesService.getTimeSeriesMetricDefinitions(cvConfigId));
  }

  @GET
  @Path("/metric-group-data")
  @Timed
  @ExceptionMetered
  public RestResponse<TimeSeriesTestDataDTO> getMetricDefinitions(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("cvConfigId") @NotNull String cvConfigId,
      @QueryParam("startTimeEpochMillis") @NotNull Long startTimeEpochMillis,
      @QueryParam("endTimeEpochMillis") @NotNull Long endTimeEpochMillis,
      @QueryParam("metricName") @NotNull String metricName,
      @QueryParam("groupNameList") @NotNull List<String> groupNameList) {
    return new RestResponse<>(
        timeSeriesService.getMetricGroupDataForRange(cvConfigId, Instant.ofEpochMilli(startTimeEpochMillis),
            Instant.ofEpochMilli(endTimeEpochMillis), metricName, groupNameList));
  }
}
