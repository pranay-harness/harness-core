package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRequest;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/14/17.
 *
 * For api versioning see documentation of {@link NewRelicResource}.
 */
@Api(LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class SplunkResource implements LogAnalysisResource {
  @Inject private AnalysisService analysisService;

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("workflowId") String workflowId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, @QueryParam("appId") final String appId,
      @QueryParam("serviceId") String serviceId, @QueryParam("clusterLevel") ClusterLevel clusterLevel,
      @QueryParam("delegateTaskId") String delegateTaskId, List<LogElement> logData) throws IOException {
    return new RestResponse<>(analysisService.saveLogData(StateType.SPLUNKV2, accountId, appId, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, clusterLevel, delegateTaskId, logData));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<LogDataRecord>> getRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("clusterLevel") ClusterLevel clusterLevel, @QueryParam("compareCurrent") boolean compareCurrent,
      LogRequest logRequest) throws IOException {
    return new RestResponse<>(
        analysisService.getLogData(logRequest, compareCurrent, workflowExecutionId, clusterLevel, StateType.SPLUNKV2));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveLogAnalysisMLRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("logCollectionMinute") Integer logCollectionMinute,
      @QueryParam("isBaselineCreated") boolean isBaselineCreated, @QueryParam("taskId") String taskId,
      LogMLAnalysisRecord mlAnalysisResponse) throws IOException {
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setLogCollectionMinute(logCollectionMinute);
    mlAnalysisResponse.setBaseLineCreated(isBaselineCreated);
    return new RestResponse<>(
        analysisService.saveLogAnalysisRecords(mlAnalysisResponse, StateType.SPLUNKV2, Optional.of(taskId)));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<LogMLAnalysisRecord> getLogMLAnalysisRecords(
      @QueryParam("accountId") String accountId, LogMLAnalysisRequest mlAnalysisRequest) throws IOException {
    return new RestResponse<>(analysisService.getLogAnalysisRecords(mlAnalysisRequest.getApplicationId(),
        mlAnalysisRequest.getStateExecutionId(), mlAnalysisRequest.getQuery(), StateType.SPLUNKV2,
        mlAnalysisRequest.getLogCollectionMinute()));
  }

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<LogMLAnalysisSummary> getLogAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId)
      throws IOException {
    return new RestResponse<>(analysisService.getAnalysisSummary(stateExecutionId, applicationId, StateType.SPLUNKV2));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<Boolean> userFeedback(@QueryParam("accountId") String accountId, LogMLFeedback ignoreFeedback)
      throws IOException {
    return new RestResponse<>(analysisService.saveFeedback(ignoreFeedback, StateType.SPLUNKV2));
  }
}
