package io.harness.cvng.analysis.resource;

import static io.harness.cvng.CVConstants.TIMESERIES_ANALYSIS_RESOURCE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
      @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant) {
    return new RestResponse<>(timeSeriesAnalysisService.getTestData(
        cvConfigId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
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
  public RestResponse<List<TimeSeriesMetricDefinition>> getMetricTemplate(@QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(timeSeriesAnalysisService.getMetricTemplate(cvConfigId));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/timeseries-serviceguard-save-analysis")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveServiceGuardAnalysis(@QueryParam("taskId") String taskId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("analysisStartTime") String epochStartInstant,
      @QueryParam("analysisEndTime") String epochEndInstant, ServiceGuardMetricAnalysisDTO analysisBody) {
    timeSeriesAnalysisService.saveAnalysis(
        analysisBody, cvConfigId, taskId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant));
    return new RestResponse<>(true);
  }
}
