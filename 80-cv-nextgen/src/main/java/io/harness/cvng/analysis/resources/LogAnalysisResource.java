package io.harness.cvng.analysis.resources;

import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_ANALYSIS_RESOURCE;
import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_ANALYSIS_SAVE_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.PREVIOUS_LOG_ANALYSIS_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TEST_DATA_PATH;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.entities.LogAnalysisFrequencyPattern.FrequencyPattern;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(LOG_ANALYSIS_RESOURCE)
@Path(LOG_ANALYSIS_RESOURCE)
@Produces("application/json")
@Slf4j
public class LogAnalysisResource {
  @Inject LogAnalysisService logAnalysisService;

  @GET
  @Path("/" + TEST_DATA_PATH)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<LogClusterDTO>> getTestData(@QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("analysisStartTime") String analysisStartTime,
      @QueryParam("analysisEndTime") String analysisEndTime) {
    return new RestResponse<>(
        logAnalysisService.getTestData(cvConfigId, Instant.parse(analysisStartTime), Instant.parse(analysisEndTime)));
  }

  @GET
  @Path("/" + PREVIOUS_LOG_ANALYSIS_PATH)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<FrequencyPattern>> getPreviousAnalysis(@QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("analysisStartTime") String analysisStartTime,
      @QueryParam("analysisEndTime") String analysisEndTime) {
    return new RestResponse<>(logAnalysisService.getFrequencyPattern(
        cvConfigId, Instant.parse(analysisStartTime), Instant.parse(analysisEndTime)));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/" + LOG_ANALYSIS_SAVE_PATH)
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveServiceGuardAnalysis(@QueryParam("taskId") String taskId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("analysisStartTime") String analysisStartTime,
      @QueryParam("analysisEndTime") String analysisEndTime, LogAnalysisDTO analysisBody) {
    logAnalysisService.saveAnalysis(
        cvConfigId, taskId, Instant.parse(analysisStartTime), Instant.parse(analysisEndTime), analysisBody);
    return new RestResponse<>(true);
  }
}
