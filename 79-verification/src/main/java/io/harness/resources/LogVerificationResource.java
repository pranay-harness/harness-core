package io.harness.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.exception.WingsException;
import io.harness.service.intfc.LogAnalysisService;
import io.swagger.annotations.Api;
import software.wings.api.InstanceElement;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRequest;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(LogAnalysisResource.LOG_ANALYSIS)
@Path("/" + LogAnalysisResource.LOG_ANALYSIS)
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class LogVerificationResource {
  @Inject private LogAnalysisService analysisService;

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Set<LogDataRecord>> getRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("clusterLevel") ClusterLevel clusterLevel, @QueryParam("compareCurrent") boolean compareCurrent,
      @QueryParam("stateType") StateType stateType, LogRequest logRequest) {
    return new RestResponse<>(
        analysisService.getLogData(logRequest, compareCurrent, workflowExecutionId, clusterLevel, stateType));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_GET_24X7_LOG_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Set<LogDataRecord>> getRawLogData(@QueryParam("appId") String appId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("clusterLevel") ClusterLevel clusterLevel,
      @QueryParam("logCollectionMinute") int logCollectionMinute, @QueryParam("startMinute") int startMinute,
      @QueryParam("endMinute") int endMinute, LogRequest logRequest) {
    return new RestResponse<>(analysisService.getLogData(
        appId, cvConfigId, clusterLevel, logCollectionMinute, startMinute, endMinute, logRequest));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowId") String workflowId, @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("appId") final String appId, @QueryParam("serviceId") String serviceId,
      @QueryParam("clusterLevel") ClusterLevel clusterLevel, @QueryParam("delegateTaskId") String delegateTaskId,
      @QueryParam("stateType") StateType stateType, List<LogElement> logData) {
    return new RestResponse<>(analysisService.saveLogData(stateType, accountId, appId, cvConfigId, stateExecutionId,
        workflowId, workflowExecutionId, serviceId, clusterLevel, delegateTaskId, logData));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveClusteredLogData(@QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("appId") String appId, @QueryParam("clusterLevel") ClusterLevel clusterLevel,
      @QueryParam("logCollectionMinute") int logCollectionMinute, @QueryParam("host") String host,
      List<LogElement> logData) {
    return new RestResponse<>(
        analysisService.saveClusteredLogData(appId, cvConfigId, clusterLevel, logCollectionMinute, host, logData));
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
      @QueryParam("baseLineExecutionId") String baseLineExecutionId, @QueryParam("stateType") StateType stateType,
      LogMLAnalysisRecord mlAnalysisResponse) {
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setLogCollectionMinute(logCollectionMinute);
    mlAnalysisResponse.setBaseLineCreated(isBaselineCreated);
    mlAnalysisResponse.setBaseLineExecutionId(baseLineExecutionId);
    mlAnalysisResponse.setAppId(applicationId);
    return new RestResponse<>(
        analysisService.saveLogAnalysisRecords(mlAnalysisResponse, stateType, Optional.of(taskId)));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveLogAnalysisMLRecords(@QueryParam("appId") String appId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("analysisMinute") int analysisMinute,
      @QueryParam("taskId") String taskId, LogMLAnalysisRecord mlAnalysisResponse) {
    return new RestResponse<>(analysisService.save24X7LogAnalysisRecords(
        appId, cvConfigId, analysisMinute, mlAnalysisResponse, Optional.of(taskId)));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<LogMLAnalysisRecord> getLogMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("stateType") StateType stateType, LogMLAnalysisRequest mlAnalysisRequest) {
    return new RestResponse<>(analysisService.getLogAnalysisRecords(mlAnalysisRequest.getApplicationId(),
        mlAnalysisRequest.getStateExecutionId(), mlAnalysisRequest.getQuery(), stateType,
        mlAnalysisRequest.getLogCollectionMinute()));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path(LogAnalysisResource.ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<LogMLAnalysisRecord> getLogMLAnalysisRecords(@QueryParam("appId") String appId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("analysisMinute") int analysisMinute) {
    return new RestResponse<>(analysisService.getLogAnalysisRecords(appId, cvConfigId, analysisMinute));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> createUserFeedback(
      @QueryParam("accountId") String accountId, @QueryParam("stateType") StateType stateType, LogMLFeedback feedback) {
    if (!isEmpty(feedback.getLogMLFeedbackId())) {
      throw new WingsException("feedback id should not be set in POST call. to update feedback use PUT");
    }
    return new RestResponse<>(analysisService.saveFeedback(feedback, stateType));
  }

  @DELETE
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK + "/{feedbackId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteUserFeedback(
      @QueryParam("accountId") String accountId, @PathParam("feedbackId") String feedbackId) {
    return new RestResponse<>(analysisService.deleteFeedback(feedbackId));
  }

  @GET
  @Produces({"application/json", "application/v1+json"})
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<LogMLFeedbackRecord>> getFeedback(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId,
      @QueryParam("workflowId") String workflowId, @QueryParam("workflowExecutionId") String workflowExecutionId) {
    return new RestResponse<>(analysisService.getMLFeedback(appId, serviceId, workflowId, workflowExecutionId));
  }

  @GET
  @Produces({"application/json", "application/v1+json"})
  @Path(LogAnalysisResource.LAST_EXECUTION_NODES)
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, InstanceElement>> getLastExecutionNodes(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("workflowId") String workflowId) {
    return new RestResponse<>(analysisService.getLastExecutionNodes(appId, workflowId));
  }
}
