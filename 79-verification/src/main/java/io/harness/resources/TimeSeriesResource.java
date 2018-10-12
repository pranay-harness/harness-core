package io.harness.resources;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 04/11/18.
 */
@Api(MetricDataAnalysisService.RESOURCE_URL)
@Path("/" + MetricDataAnalysisService.RESOURCE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class TimeSeriesResource {
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;

  @VisibleForTesting
  @Inject
  public TimeSeriesResource(TimeSeriesAnalysisService timeSeriesAnalysisService) {
    this.timeSeriesAnalysisService = timeSeriesAnalysisService;
  }

  @POST
  @Path("/save-metrics")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricData(@QueryParam("accountId") final String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("delegateTaskId") String delegateTaskId, List<NewRelicMetricDataRecord> metricData)
      throws IOException {
    return new RestResponse<>(timeSeriesAnalysisService.saveMetricData(
        accountId, applicationId, stateExecutionId, delegateTaskId, metricData));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-metrics")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricDataRecord>> getMetricData(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("workflowExecutionId") String workFlowExecutionId, @QueryParam("groupName") final String groupName,
      @QueryParam("compareCurrent") boolean compareCurrent, TSRequest request) throws IOException {
    if (compareCurrent) {
      return new RestResponse<>(timeSeriesAnalysisService.getRecords(stateType, appId, request.getWorkflowExecutionId(),
          request.getStateExecutionId(), request.getWorkflowId(), request.getServiceId(), groupName, request.getNodes(),
          request.getAnalysisMinute(), request.getAnalysisStartMinute()));
    } else {
      if (workFlowExecutionId == null || workFlowExecutionId.equals("-1")) {
        return new RestResponse<>(new ArrayList<>());
      }

      return new RestResponse<>(timeSeriesAnalysisService.getPreviousSuccessfulRecords(stateType, appId,
          request.getWorkflowId(), workFlowExecutionId, request.getServiceId(), groupName, request.getAnalysisMinute(),
          request.getAnalysisStartMinute()));
    }
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/save-analysis")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateType") StateType stateType,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("workflowId") final String workflowId, @QueryParam("serviceId") final String serviceId,
      @QueryParam("groupName") final String groupName, @QueryParam("analysisMinute") Integer analysisMinute,
      @QueryParam("taskId") String taskId, @QueryParam("baseLineExecutionId") String baseLineExecutionId,
      @QueryParam("cvConfigId") String cvConfigId, TimeSeriesMLAnalysisRecord mlAnalysisResponse) {
    return new RestResponse<>(timeSeriesAnalysisService.saveAnalysisRecordsML(stateType, accountId, applicationId,
        stateExecutionId, workflowExecutionId, workflowId, serviceId, groupName, analysisMinute, taskId,
        baseLineExecutionId, cvConfigId, mlAnalysisResponse));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-scores")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<TimeSeriesMLScores>> getScores(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("workFlowId") String workflowId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("limit") Integer limit) throws IOException {
    return new RestResponse<>(
        timeSeriesAnalysisService.getTimeSeriesMLScores(applicationId, workflowId, analysisMinute, limit));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-metric-template")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> getMetricTemplate(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("stateType") StateType stateType, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName) {
    return new RestResponse<>(
        timeSeriesAnalysisService.getMetricTemplate(appId, stateType, stateExecutionId, serviceId, groupName));
  }

  @GET
  @Path("/threshold")
  @Timed
  @ExceptionMetered
  public RestResponse<TimeSeriesMLTransactionThresholds> getCustomThreshold(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName,
      @QueryParam("transactionName") String transactionName, @QueryParam("metricName") String metricName) {
    return new RestResponse<>(timeSeriesAnalysisService.getCustomThreshold(
        appId, stateType, serviceId, groupName, transactionName, metricName));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-metric-data-247")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricDataRecord>> getMetricRecords(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("serviceId") String serviceId,
      @QueryParam("analysisStartMin") int analysisStartMin, @QueryParam("analysisEndMin") int analysisEndMin,
      TSRequest request) {
    return new RestResponse<>(timeSeriesAnalysisService.getMetricRecords(
        stateType, appId, serviceId, cvConfigId, analysisStartMin, analysisEndMin));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-metric-template_24_7")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> getMetricTemplate(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("stateType") StateType stateType, @QueryParam("serviceId") String serviceId,
      @QueryParam("groupName") String groupName) {
    return new RestResponse<>(timeSeriesAnalysisService.getMetricTemplate(appId, stateType, serviceId, groupName));
  }
}
