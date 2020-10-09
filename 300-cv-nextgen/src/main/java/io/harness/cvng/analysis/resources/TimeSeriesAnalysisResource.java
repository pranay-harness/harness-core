package io.harness.cvng.analysis.resources;

import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_ANALYSIS_RESOURCE;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SAVE_ANALYSIS_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_VERIFICATION_TASK_SAVE_ANALYSIS_PATH;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(TIMESERIES_ANALYSIS_RESOURCE)
@Path(TIMESERIES_ANALYSIS_RESOURCE)
@Produces("application/json")
@Slf4j
public class TimeSeriesAnalysisResource {
  @Inject TimeSeriesAnalysisService timeSeriesAnalysisService;

  @GET
  @Path("/time-series-data")
  @Timed
  @ExceptionMetered
  @ApiOperation(
      value = "get test timeseries data for a verification job and risk analysis", nickname = "getTimeSeriesRecords")
  public RestResponse<List<TimeSeriesRecordDTO>>
  getTimeSeriesRecords(@QueryParam("verificationTaskId") @NotNull String verificationTaskId,
      @QueryParam("startTime") @NotNull Long startTime, @QueryParam("endTime") @NotNull Long endTime) {
    return new RestResponse<>(timeSeriesAnalysisService.getTimeSeriesRecordDTOs(
        verificationTaskId, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)));
  }

  @GET
  @Path("/timeseries-serviceguard-cumulative-sums")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get risk analysis cumulative sums", nickname = "getCumulativeSums")
  public RestResponse<Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>>> getCumulativeSums(
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant) {
    return new RestResponse<>(timeSeriesAnalysisService.getCumulativeSums(
        cvConfigId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
  }

  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path("/timeseries-serviceguard-previous-anomalies")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  @ApiOperation(value = "get previous anomalies for a data source config", nickname = "getPreviousAnomalies")
  public RestResponse<Map<String, Map<String, List<TimeSeriesAnomalies>>>> getPreviousAnomalies(
      @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(timeSeriesAnalysisService.getLongTermAnomalies(cvConfigId));
  }

  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path("/timeseries-serviceguard-shortterm-history")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  @ApiOperation(value = "get short term history for a data source config", nickname = "getShortTermHistory")
  public RestResponse<Map<String, Map<String, List<Double>>>> getShortTermHistory(
      @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(timeSeriesAnalysisService.getShortTermHistory(cvConfigId));
  }

  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path("/timeseries-serviceguard-metric-template")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  @ApiOperation(value = "get metric template for a verification job", nickname = "getMetricTimeSeriesTemplate")
  public RestResponse<List<TimeSeriesMetricDefinition>> getMetricTemplate(
      @QueryParam("verificationTaskId") String verificationTaskId) {
    return new RestResponse<>(timeSeriesAnalysisService.getMetricTemplate(verificationTaskId));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(TIMESERIES_SAVE_ANALYSIS_PATH)
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  @ApiOperation(
      value = "save time series analysis for a data source config", nickname = "saveServiceGuardTimeseriesAnalysis")
  public RestResponse<Boolean>
  saveServiceGuardAnalysis(@QueryParam("taskId") String taskId, ServiceGuardMetricAnalysisDTO analysisBody) {
    timeSeriesAnalysisService.saveAnalysis(taskId, analysisBody);
    return new RestResponse<>(true);
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(TIMESERIES_VERIFICATION_TASK_SAVE_ANALYSIS_PATH)
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  @ApiOperation(
      value = "save time series analysis for a deployment verification", nickname = "saveVerificationTaskAnalysis")
  public RestResponse<Void>
  saveVerificationTaskAnalysis(@QueryParam("taskId") String taskId, @QueryParam("endTime") long endTime,
      DeploymentTimeSeriesAnalysisDTO analysisBody) {
    timeSeriesAnalysisService.saveAnalysis(taskId, analysisBody);
    return new RestResponse<>(null);
  }
}
