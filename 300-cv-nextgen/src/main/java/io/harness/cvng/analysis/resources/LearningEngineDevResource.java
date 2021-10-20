package io.harness.cvng.analysis.resources;

import static io.harness.cvng.analysis.CVAnalysisConstants.LE_DEV_RESOURCE;

import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.entities.ClusteredLog;
import io.harness.cvng.analysis.entities.LogAnalysisRecord;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.analysis.services.api.LearningEngineDevService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api(LE_DEV_RESOURCE)
@Path(LE_DEV_RESOURCE)
@Produces("application/json")
@Slf4j
public class LearningEngineDevResource {
  /*
    All the APIs in this Resource are exclusively written for LE developmental requirements and for experimentation.
    If your API is not needed by our ML team for experiments, please do not add it here.
 */

  @Inject LearningEngineDevService learningEngineDevService;

  @GET
  @Path("/timeseries-risk-summary-by-time")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get risk analysis cumulative sums", nickname = "getRiskSummariesByTimeRange")
  public RestResponse<List<TimeSeriesRiskSummary>> getRiskSummariesByTimeRange(
      @QueryParam("verificationTaskId") String verificationTaskId,
      @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant) {
    return new RestResponse<>(learningEngineDevService.getRiskSummariesByTimeRange(
        verificationTaskId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
  }

  @GET
  @Path("/timeseries-anom-patterns-by-id")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get risk analysis cumulative sums", nickname = "getAnomalousPatternsById")
  public RestResponse<TimeSeriesAnomalousPatterns> getAnomalousPatternsById(
      @QueryParam("verificationTaskId") String verificationTaskId) {
    return new RestResponse<>(learningEngineDevService.getTimeSeriesAnomalousPatternsByTaskId(verificationTaskId));
  }

  @GET
  @Path("/timeseries-short-term-history-by-id")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get risk analysis cumulative sums", nickname = "getTimeSeriesShortTermHistoryByTaskId")
  public RestResponse<TimeSeriesShortTermHistory> getTimeSeriesShortTermHistoryByTaskId(
      @QueryParam("verificationTaskId") String verificationTaskId) {
    return new RestResponse<>(learningEngineDevService.getTimeSeriesShortTermHistoryByTaskId(verificationTaskId));
  }

  @GET
  @Path("/timeseries-cumulative-sums-by-time")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get risk analysis cumulative sums", nickname = "getTimeSeriesCumulativeSumsByTimeRange")
  public RestResponse<List<TimeSeriesCumulativeSums>> getTimeSeriesCumulativeSumsByTimeRange(
      @QueryParam("verificationTaskId") String verificationTaskId,
      @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant) {
    return new RestResponse<>(learningEngineDevService.getTimeSeriesCumulativeSumsByTimeRange(
        verificationTaskId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
  }

  @GET
  @Path("/log-analysis-by-time")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get risk analysis cumulative sums", nickname = "getLogAnalysisRecordsByTimeRange")
  public RestResponse<List<LogAnalysisRecord>> getLogAnalysisRecordsByTimeRange(
      @QueryParam("verificationTaskId") String verificationTaskId,
      @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant) {
    return new RestResponse<>(learningEngineDevService.getLogAnalysisRecordsByTimeRange(
        verificationTaskId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
  }

  @GET
  @Path("/log-analysis-result-by-time")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get risk analysis cumulative sums", nickname = "getLogAnalysisResultByTimeRange")
  public RestResponse<List<LogAnalysisResult>> getLogAnalysisResultByTimeRange(
      @QueryParam("verificationTaskId") String verificationTaskId,
      @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant) {
    return new RestResponse<>(learningEngineDevService.getLogAnalysisResultByTimeRange(
        verificationTaskId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
  }

  @GET
  @Path("/clustered-logs-by-time")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get risk analysis cumulative sums", nickname = "getClusteredLogsByTimeRange")
  public RestResponse<List<ClusteredLog>> getClusteredLogsByTimeRange(
      @QueryParam("verificationTaskId") String verificationTaskId,
      @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant) {
    return new RestResponse<>(learningEngineDevService.getClusteredLogsByTimeRange(
        verificationTaskId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
  }

  @GET
  @Path("/time-series-data")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get test timeseries data for a verification job and risk analysis",
      nickname = "getTimeSeriesRecordsForLE")
  public RestResponse<List<TimeSeriesRecordDTO>>
  getTimeSeriesRecordsForLE(@QueryParam("verificationTaskId") @NotNull String verificationTaskId,
      @QueryParam("startTime") @NotNull Long startTime, @QueryParam("endTime") @NotNull Long endTime) {
    return new RestResponse<>(learningEngineDevService.getTimeSeriesRecordDTOs(
        verificationTaskId, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)));
  }
}
