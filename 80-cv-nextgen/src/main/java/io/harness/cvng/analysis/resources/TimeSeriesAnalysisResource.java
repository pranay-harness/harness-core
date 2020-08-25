package io.harness.cvng.analysis.resources;

import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_ANALYSIS_RESOURCE;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SAVE_ANALYSIS_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_VERIFICATION_TASK_SAVE_ANALYSIS_PATH;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.beans.DeploymentVerificationTaskTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.beans.TimeSeriesRecordDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;
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
  @Path("/timeseries-serviceguard-test-data")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, Map<String, List<Double>>>> getTestData(@QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("verificationTaskId") String verificationTaskId,
      @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant) {
    if (verificationTaskId == null) {
      verificationTaskId = cvConfigId;
    }
    return new RestResponse<>(timeSeriesAnalysisService.getTestData(
        verificationTaskId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
  }

  @GET
  @Path("/time-series-data")
  @Timed
  @ExceptionMetered
  public RestResponse<List<TimeSeriesRecordDTO>> getTimeSeriesRecords(
      @QueryParam("verificationTaskId") @NotNull String verificationTaskId,
      @QueryParam("startTime") @NotNull Long startTime, @QueryParam("endTime") @NotNull Long endTime) {
    return new RestResponse<>(timeSeriesAnalysisService.getTimeSeriesRecordDTOs(
        verificationTaskId, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)));
  }

  @GET
  @Path("/timeseries-serviceguard-cumulative-sums")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
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
  public RestResponse<Boolean> saveServiceGuardAnalysis(
      @QueryParam("taskId") String taskId, ServiceGuardMetricAnalysisDTO analysisBody) {
    timeSeriesAnalysisService.saveAnalysis(taskId, analysisBody);
    return new RestResponse<>(true);
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(TIMESERIES_VERIFICATION_TASK_SAVE_ANALYSIS_PATH)
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<Void> saveVerificationTaskAnalysis(@QueryParam("taskId") String taskId,
      @QueryParam("endTime") long endTime, DeploymentVerificationTaskTimeSeriesAnalysisDTO analysisBody) {
    timeSeriesAnalysisService.saveAnalysis(taskId, analysisBody);
    return new RestResponse<>(null);
  }
}
