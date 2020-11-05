package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@ExposeInternalException
@NextGenManagerAuth
public class MetricPackResource {
  @Inject private MetricPackService metricPackService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get all metric packs for a connector type", nickname = "getMetricPacks")
  public RestResponse<List<MetricPackDTO>> getMetricPacks(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("dataSourceType") @NotNull DataSourceType dataSourceType) {
    return new RestResponse<>(metricPackService.getMetricPacks(dataSourceType, accountId, projectIdentifier));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves a metric pack for a connector type", nickname = "saveMetricPacks")
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
  @ApiOperation(value = "get custom thresholds for a given metric pack", nickname = "getMetricPackThresholds")
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
  @ApiOperation(value = "saves custom thresholds for a given metric pack", nickname = "saveMetricPackThresholds")
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
  @ApiOperation(value = "deletes custom thresholds for a given metric pack", nickname = "deleteMetricPackThresholds")
  public RestResponse<Boolean> deleteMetricPackThresholds(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("thresholdId") @NotNull String thresholdId) {
    return new RestResponse<>(metricPackService.deleteMetricPackThresholds(accountId, projectIdentifier, thresholdId));
  }
}
