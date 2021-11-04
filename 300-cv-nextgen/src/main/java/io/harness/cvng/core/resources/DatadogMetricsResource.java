package io.harness.cvng.core.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDetail;
import io.harness.cvng.core.services.api.DatadogService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;
import java.util.Set;

@Api("datadog-metrics")
@Path("/datadog-metrics")
@Produces("application/json")
@NextGenManagerAuth
@ExposeInternalException
@ApiResponses(value =
        {
                @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
                , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
        })
public class DatadogMetricsResource {
    @Inject
    private DatadogService datadogMetricsService;

    @GET
    @Path("/dashboards")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "get all datadog dashboards", nickname = "getDatadogDashboards")
    public ResponseDTO<PageResponse<DatadogDashboardDTO>> getDatadogDashboards(
            @NotNull @QueryParam("accountId") String accountId,
            @NotNull @QueryParam("connectorIdentifier") String connectorIdentifier,
            @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
            @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("pageSize") @NotNull int pageSize,
            @QueryParam("offset") @NotNull int offset, @QueryParam("filter") String filter,
            @NotNull @QueryParam("tracingId") String tracingId) {
        return ResponseDTO.newResponse(datadogMetricsService.getAllDashboards(
                accountId, connectorIdentifier, orgIdentifier, projectIdentifier, pageSize, offset, filter, tracingId));
    }

    @GET
    @Path("/dashboard-details")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "get datadog dashboard details", nickname = "getDatadogDashboardDetails")
    public ResponseDTO<List<DatadogDashboardDetail>> getDatadogDashboardDetails(
            @NotNull @QueryParam("dashboardId") String dashboardId,
            @NotNull @QueryParam("accountId") String accountId,
            @NotNull @QueryParam("connectorIdentifier") String connectorIdentifier,
            @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
            @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
            @NotNull @QueryParam("tracingId") String tracingId) {
        return ResponseDTO.newResponse(datadogMetricsService.getDashboardDetails(
                dashboardId, accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId));
    }

    @GET
    @Path("/active-metrics")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "get datadog active metrics", nickname = "getDatadogActiveMetrics")
    public ResponseDTO<List<String>> getDatadogMetricTagsList(
            @NotNull @QueryParam("accountId") String accountId,
            @NotNull @QueryParam("connectorIdentifier") String connectorIdentifier,
            @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
            @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
            @NotNull @QueryParam("tracingId") String tracingId) {
        return ResponseDTO.newResponse(datadogMetricsService.getActiveMetrics(
                accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId));
    }

    @GET
    @Path("/metric-tags")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "get datadog metric tag list", nickname = "getDatadogMetricTagsList")
    public ResponseDTO<List<String>> getDatadogMetricTagsList(
            @NotNull @QueryParam("metric") String metricName,
            @NotNull @QueryParam("accountId") String accountId,
            @NotNull @QueryParam("connectorIdentifier") String connectorIdentifier,
            @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
            @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
            @NotNull @QueryParam("tracingId") String tracingId) {
        return ResponseDTO.newResponse(datadogMetricsService.getMetricTagsList(
                metricName, accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId));
    }

    @GET
    @Path("/sample-data")
    @Timed
    @ExceptionMetered
    @ApiOperation(value = "get datadog sample data", nickname = "getDatadogSampleData")
    public ResponseDTO<List<TimeSeriesSampleDTO>> getDatadogSampleData(
            @NotNull @QueryParam("accountId") String accountId,
            @NotNull @QueryParam("connectorIdentifier") String connectorIdentifier,
            @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
            @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
            @NotNull @QueryParam("tracingId") String tracingId,
            @NotNull @QueryParam("query") String query) {
        return ResponseDTO.newResponse(datadogMetricsService.getTimeSeriesPoints(
                accountId, connectorIdentifier, orgIdentifier, projectIdentifier, query, tracingId));
    }
}
