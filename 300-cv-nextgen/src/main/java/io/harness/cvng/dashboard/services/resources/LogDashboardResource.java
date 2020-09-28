package io.harness.cvng.dashboard.services.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO;
import io.harness.cvng.dashboard.beans.LogDataByTag;
import io.harness.cvng.dashboard.services.api.LogDashboardService;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;

import java.util.SortedSet;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("log-dashboard")
@Path("/log-dashboard")
@Produces("application/json")
@ExposeInternalException
public class LogDashboardResource {
  @Inject private LogDashboardService logDashboardService;

  @GET
  @Path("/anomalous-logs")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
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
  @LearningEngineAuth
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
  @LearningEngineAuth
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
}
