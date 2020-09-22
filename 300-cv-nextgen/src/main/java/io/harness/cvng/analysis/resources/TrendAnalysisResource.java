package io.harness.cvng.analysis.resources;

import static io.harness.cvng.analysis.CVAnalysisConstants.TREND_ANALYSIS_RESOURCE;
import static io.harness.cvng.analysis.CVAnalysisConstants.TREND_ANALYSIS_SAVE_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TREND_ANALYSIS_TEST_DATA;
import static io.harness.cvng.analysis.CVAnalysisConstants.TREND_METRIC_TEMPLATE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.services.api.TrendAnalysisService;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
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

@Api(TREND_ANALYSIS_RESOURCE)
@Path(TREND_ANALYSIS_RESOURCE)
@Produces("application/json")
@Slf4j
public class TrendAnalysisResource {
  @Inject private TrendAnalysisService trendAnalysisService;

  @GET
  @Path("/" + TREND_ANALYSIS_TEST_DATA)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, Map<String, List<Double>>>> getTestData(
      @NotNull @QueryParam("verificationTaskId") String verificationTaskId,
      @NotNull @QueryParam("analysisStartTime") String epochStartInstant,
      @NotNull @QueryParam("analysisEndTime") String epochEndInstant) {
    return new RestResponse<>(trendAnalysisService.getTestData(
        verificationTaskId, Instant.parse(epochStartInstant), Instant.parse(epochEndInstant)));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/" + TREND_ANALYSIS_SAVE_PATH)
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<Void> saveServiceGuardAnalysis(
      @NotNull @QueryParam("taskId") String taskId, ServiceGuardMetricAnalysisDTO analysisBody) {
    trendAnalysisService.saveAnalysis(taskId, analysisBody);
    return new RestResponse<>(null);
  }

  @GET
  @Path("/" + TREND_METRIC_TEMPLATE)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<TimeSeriesMetricDefinition>> getMetricTemplate() {
    return new RestResponse<>(trendAnalysisService.getTimeSeriesMetricDefinitions());
  }
}
