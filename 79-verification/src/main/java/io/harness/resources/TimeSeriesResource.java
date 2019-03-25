package io.harness.resources;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.swagger.annotations.Api;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;

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
      @QueryParam("delegateTaskId") String delegateTaskId, List<NewRelicMetricDataRecord> metricData) {
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
      @QueryParam("appId") String appId, @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("groupName") final String groupName, @QueryParam("compareCurrent") boolean compareCurrent,
      TSRequest request) {
    if (compareCurrent) {
      return new RestResponse<>(timeSeriesAnalysisService.getRecords(appId, request.getStateExecutionId(), groupName,
          request.getNodes(), request.getAnalysisMinute(), request.getAnalysisStartMinute()));
    } else {
      if (workflowExecutionId == null || workflowExecutionId.equals("-1")) {
        return new RestResponse<>(new ArrayList<>());
      }

      return new RestResponse<>(timeSeriesAnalysisService.getPreviousSuccessfulRecords(
          appId, workflowExecutionId, groupName, request.getAnalysisMinute(), request.getAnalysisStartMinute()));
    }
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/save-analysis")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("groupName") final String groupName, @QueryParam("analysisMinute") Integer analysisMinute,
      @QueryParam("taskId") String taskId, @QueryParam("baseLineExecutionId") String baseLineExecutionId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("tag") String tag,
      TimeSeriesMLAnalysisRecord mlAnalysisResponse) {
    return new RestResponse<>(timeSeriesAnalysisService.saveAnalysisRecordsML(accountId, stateType, appId,
        stateExecutionId, workflowExecutionId, groupName, analysisMinute, taskId, baseLineExecutionId, cvConfigId,
        mlAnalysisResponse, tag));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-scores")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<TimeSeriesMLScores>> getScores(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("workflowId") String workflowId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("limit") Integer limit) {
    return new RestResponse<>(
        timeSeriesAnalysisService.getTimeSeriesMLScores(applicationId, workflowId, analysisMinute, limit));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-metric-template")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Deprecated
  public RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> getMetricTemplatePost(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("stateType") StateType stateType, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("serviceId") String serviceId, @QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("groupName") String groupName) {
    return new RestResponse<>(timeSeriesAnalysisService.getMetricTemplate(
        appId, stateType, stateExecutionId, serviceId, cvConfigId, groupName));
  }

  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path("/get-metric-template")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> getMetricTemplate(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId,
      @QueryParam("stateType") StateType stateType, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("serviceId") String serviceId, @QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("groupName") String groupName) {
    return new RestResponse<>(timeSeriesAnalysisService.getMetricTemplate(
        appId, stateType, stateExecutionId, serviceId, cvConfigId, groupName));
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
      @QueryParam("tag") String tag, TSRequest request) {
    return new RestResponse<>(timeSeriesAnalysisService.getMetricRecords(
        stateType, appId, serviceId, cvConfigId, analysisStartMin, analysisEndMin, tag));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/previous-analysis-247")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<TimeSeriesMLAnalysisRecord> getPreviousAnalysis(@QueryParam("appId") String appId,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("dataCollectionMin") long dataCollectionMin,
      @QueryParam("tag") String tag) {
    return new RestResponse<>(timeSeriesAnalysisService.getPreviousAnalysis(appId, cvConfigId, dataCollectionMin, tag));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/historical-analysis-24x7")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<List<TimeSeriesMLAnalysisRecord>> getHistoricalAnalysis(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String appId, @QueryParam("analysisMinute") Integer analysisMinute,
      @QueryParam("serviceId") String serviceId, @QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("tag") String tag) {
    return new RestResponse<>(
        timeSeriesAnalysisService.getHistoricalAnalysis(accountId, appId, serviceId, cvConfigId, analysisMinute, tag));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/previous-anomalies-247")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<TimeSeriesAnomaliesRecord> getPreviousAnomalies(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String appId, @QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("tag") String tag, Map<String, List<String>> metrics) {
    return new RestResponse<>(timeSeriesAnalysisService.getPreviousAnomalies(appId, cvConfigId, metrics, tag));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/cumulative-sums-247")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<List<TimeSeriesCumulativeSums>> getCumulativeSums(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String appId, @QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("analysisMinStart") Integer startMinute, @QueryParam("analysisMinEnd") Integer endMinute,
      @QueryParam("tag") String tag) {
    return new RestResponse<>(
        timeSeriesAnalysisService.getCumulativeSumsForRange(appId, cvConfigId, startMinute, endMinute, tag));
  }
}
