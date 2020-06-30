package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.core.services.entities.TimeSeriesThreshold;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/metric-pack")
@Path("/metric-pack")
@Produces("application/json")
public class MetricPackResource {
  @Inject private MetricPackService metricPackService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<MetricPack>> getMetricPacks(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("dataSourceType") @NotNull DataSourceType dataSourceType) {
    return new RestResponse<>(metricPackService.getMetricPacks(accountId, projectIdentifier, dataSourceType));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricPacks(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("dataSourceType") @NotNull DataSourceType dataSourceType,
      @NotNull @Valid @Body List<MetricPack> metricPacks) {
    return new RestResponse<>(
        metricPackService.saveMetricPacks(accountId, projectIdentifier, dataSourceType, metricPacks));
  }

  @GET
  @Path("/thresholds")
  @Timed
  @ExceptionMetered
  public RestResponse<List<TimeSeriesThreshold>> getMetricPackThresholds(
      @QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("metricPackIdentifier") @NotNull String metricPackIdentifier,
      @QueryParam("dataSourceType") @NotNull DataSourceType dataSourceType) {
    return new RestResponse<>(
        metricPackService.getMetricPackThresholds(accountId, projectIdentifier, metricPackIdentifier, dataSourceType));
  }

  @POST
  @Path("/thresholds")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> saveMetricPackThresholds(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("dataSourceType") @NotNull DataSourceType dataSourceType,
      @NotNull @Valid @Body List<TimeSeriesThreshold> timeSeriesThresholds) {
    return new RestResponse<>(
        metricPackService.saveMetricPackThreshold(accountId, projectIdentifier, dataSourceType, timeSeriesThresholds));
  }

  @DELETE
  @Path("/thresholds")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteMetricPackThresholds(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("thresholdId") @NotNull String thresholdId) {
    return new RestResponse<>(metricPackService.deleteMetricPackThresholds(accountId, projectIdentifier, thresholdId));
  }
}
