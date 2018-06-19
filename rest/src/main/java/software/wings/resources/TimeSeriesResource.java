package software.wings.resources;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.APMFetchConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.RestResponse;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
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
  @Inject private NewRelicService newRelicService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @POST
  @Path("/save-metrics")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricData(@QueryParam("accountId") final String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("delegateTaskId") String delegateTaskId, List<NewRelicMetricDataRecord> metricData)
      throws IOException {
    return new RestResponse<>(metricDataAnalysisService.saveMetricData(
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
      return new RestResponse<>(metricDataAnalysisService.getRecords(stateType, appId, request.getWorkflowExecutionId(),
          request.getStateExecutionId(), request.getWorkflowId(), request.getServiceId(), groupName, request.getNodes(),
          request.getAnalysisMinute(), request.getAnalysisStartMinute()));
    } else {
      if (workFlowExecutionId == null || workFlowExecutionId.equals("-1")) {
        return new RestResponse<>(new ArrayList<>());
      }

      return new RestResponse<>(metricDataAnalysisService.getPreviousSuccessfulRecords(stateType, appId,
          request.getWorkflowId(), workFlowExecutionId, request.getServiceId(), groupName, request.getAnalysisMinute(),
          request.getAnalysisStartMinute()));
    }
  }

  @GET
  @Path("/generate-metrics")
  @Timed
  @ExceptionMetered
  public RestResponse<NewRelicMetricAnalysisRecord> getMetricsAnalysis(
      @QueryParam("stateExecutionId") final String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("accountId") final String accountId, @QueryParam("appId") final String appId) throws IOException {
    if (featureFlagService.isEnabledRelaodCache(FeatureName.CV_DEMO, accountId)) {
      return new RestResponse<>(
          metricDataAnalysisService.getMetricsAnalysisForDemo(appId, stateExecutionId, workflowExecutionId).get(0));
    } else {
      return new RestResponse<>(
          metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId).get(0));
    }
  }

  @GET
  @Path("/generate-metrics-appdynamics")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricAnalysisRecord>> getMetricsAnalysisAppdynamics(
      @QueryParam("stateExecutionId") final String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("accountId") final String accountId, @QueryParam("appId") final String appId) throws IOException {
    if (featureFlagService.isEnabledRelaodCache(FeatureName.CV_DEMO, accountId)) {
      return new RestResponse<>(Lists.newArrayList(
          metricDataAnalysisService.getMetricsAnalysisForDemo(appId, stateExecutionId, workflowExecutionId).get(0)));
    }
    return new RestResponse<>(
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId));
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
      TimeSeriesMLAnalysisRecord mlAnalysisResponse) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.saveAnalysisRecordsML(stateType, accountId, applicationId,
        stateExecutionId, workflowExecutionId, workflowId, serviceId, groupName, analysisMinute, taskId,
        baseLineExecutionId, mlAnalysisResponse));
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
        metricDataAnalysisService.getTimeSeriesMLScores(applicationId, workflowId, analysisMinute, limit));
  }

  @POST
  @Path("/get-tooltip")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricHostAnalysisValue>> getTooltipPost(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workFlowExecutionId") String workFlowExecutionId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName, @QueryParam("groupName") String groupName) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.getToolTip(
        stateExecutionId, workFlowExecutionId, analysisMinute, transactionName, metricName, groupName));
  }

  @GET
  @Path("/get-tooltip")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricHostAnalysisValue>> getTooltip(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workFlowExecutionId") String workFlowExecutionId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName, @QueryParam("groupName") String groupName) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.getToolTip(
        stateExecutionId, workFlowExecutionId, analysisMinute, transactionName, metricName, groupName));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-metric-template")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(
      @QueryParam("accountId") String accountId, @QueryParam("stateType") StateType stateType,
      @QueryParam("stateExecutionId") String stateExecutionId) {
    return new RestResponse<>(metricDataAnalysisService.getMetricTemplate(stateType, stateExecutionId));
  }

  @POST
  @Path("/fetch")
  @Timed
  @ExceptionMetered
  public RestResponse<String> fetch(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String serverConfigId, APMFetchConfig fetchConfig) {
    return new RestResponse<>(newRelicService.fetch(accountId, serverConfigId, fetchConfig));
  }
}
