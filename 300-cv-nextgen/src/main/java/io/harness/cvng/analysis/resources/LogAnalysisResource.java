package io.harness.cvng.analysis.resources;

import static io.harness.cvng.analysis.CVAnalysisConstants.DEPLOYMENT_LOG_ANALYSIS_SAVE_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_ANALYSIS_RESOURCE;
import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_ANALYSIS_SAVE_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.PREVIOUS_LOG_ANALYSIS_PATH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TEST_DATA_PATH;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;
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
  @ApiOperation(value = "get test log data for a verification job", nickname = "getTestLogData")
  public RestResponse<List<LogClusterDTO>> getTestData(@QueryParam("verificationTaskId") String verificationTaskId,
      @NotNull @QueryParam("analysisStartTime") Long analysisStartTime,
      @NotNull @QueryParam("analysisEndTime") Long analysisEndTime) {
    return new RestResponse<>(logAnalysisService.getTestData(
        verificationTaskId, Instant.ofEpochMilli(analysisStartTime), Instant.ofEpochMilli(analysisEndTime)));
  }

  @GET
  @Path("/" + PREVIOUS_LOG_ANALYSIS_PATH)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @ApiOperation(value = "get previous log analysis for a data source config", nickname = "getPreviousLogAnalysis")
  public RestResponse<List<LogAnalysisCluster>> getPreviousAnalysis(
      @QueryParam("verificationTaskId") String verificationTaskId,
      @QueryParam("analysisStartTime") String analysisStartTime,
      @QueryParam("analysisEndTime") String analysisEndTime) {
    return new RestResponse<>(logAnalysisService.getPreviousAnalysis(
        verificationTaskId, Instant.parse(analysisStartTime), Instant.parse(analysisEndTime)));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/" + LOG_ANALYSIS_SAVE_PATH)
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  @ApiOperation(value = "saves log risk analysis for a data source config", nickname = "saveServiceGuardLogAnalysis")
  public RestResponse<Boolean> saveServiceGuardAnalysis(
      @QueryParam("taskId") String taskId, LogAnalysisDTO analysisBody) {
    logAnalysisService.saveAnalysis(taskId, analysisBody);
    return new RestResponse<>(true);
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/" + DEPLOYMENT_LOG_ANALYSIS_SAVE_PATH)
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  @ApiOperation(value = "saves log risk analysis for a deployment verification", nickname = "saveDeploymentAnalysis")
  public RestResponse<Void> saveDeploymentAnalysis(
      @QueryParam("taskId") String taskId, DeploymentLogAnalysisDTO deploymentLogAnalysisDTO) {
    logAnalysisService.saveAnalysis(taskId, deploymentLogAnalysisDTO);
    return new RestResponse<>(null);
  }
}
