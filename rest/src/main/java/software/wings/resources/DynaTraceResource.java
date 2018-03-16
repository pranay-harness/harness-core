package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.MetricAnalysisResource;
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
 * Created by rsingh on 2/6/18.
 */
@Api("dynatrace")
@Path("/dynatrace")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class DynaTraceResource implements MetricAnalysisResource {
  @Inject private MetricDataAnalysisService metricDataAnalysisService;
  @Inject private LearningEngineService learningEngineService;

  @POST
  @Path("/save-metrics")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  @Override
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
  @Override
  public RestResponse<List<NewRelicMetricDataRecord>> getMetricData(@QueryParam("accountId") String accountId,
      @QueryParam("workflowExecutionId") String workFlowExecutionId,
      @QueryParam("compareCurrent") boolean compareCurrent, TSRequest request) throws IOException {
    List<NewRelicMetricDataRecord> metricDataRecords;
    if (compareCurrent) {
      metricDataRecords = metricDataAnalysisService.getRecords(StateType.DYNA_TRACE, request.getWorkflowExecutionId(),
          request.getStateExecutionId(), request.getWorkflowId(), request.getServiceId(), request.getNodes(),
          request.getAnalysisMinute(), request.getAnalysisStartMinute());
    } else {
      if (workFlowExecutionId == null || workFlowExecutionId.equals("-1")) {
        metricDataRecords = new ArrayList<>();
      } else {
        metricDataRecords = metricDataAnalysisService.getPreviousSuccessfulRecords(StateType.DYNA_TRACE,
            request.getWorkflowId(), workFlowExecutionId, request.getServiceId(), request.getAnalysisMinute(),
            request.getAnalysisStartMinute());
      }
    }

    metricDataRecords.forEach(metricDataRecord -> { metricDataRecord.setHost(metricDataRecord.getName()); });
    return new RestResponse<>(metricDataRecords);
  }

  @GET
  @Path("/generate-metrics")
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<NewRelicMetricAnalysisRecord> getMetricsAnalysis(
      @QueryParam("stateExecutionId") final String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("accountId") final String accountId) throws IOException {
    NewRelicMetricAnalysisRecord metricsAnalysis =
        metricDataAnalysisService.getMetricsAnalysis(StateType.DYNA_TRACE, stateExecutionId, workflowExecutionId);
    if (metricsAnalysis == null || metricsAnalysis.getMetricAnalyses() == null) {
      return new RestResponse<>(metricsAnalysis);
    }
    for (NewRelicMetricAnalysis analysis : metricsAnalysis.getMetricAnalyses()) {
      String metricName = analysis.getMetricName();
      String[] split = metricName.split(":");
      if (split == null || split.length == 1) {
        analysis.setDisplayName(metricName);
        analysis.setFullMetricName(metricName);
        continue;
      }
      String btName = split[0];
      String fullBTName = btName + " (" + metricName.substring(btName.length() + 1) + ")";
      analysis.setDisplayName(btName);
      analysis.setFullMetricName(fullBTName);
    }
    return new RestResponse<>(metricsAnalysis);
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/save-analysis")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Override
  public RestResponse<Boolean> saveMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("workflowId") final String workflowId, @QueryParam("serviceId") String serviceId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("taskId") String taskId,
      @QueryParam("baseLineExecutionId") String baseLineExecutionId, TimeSeriesMLAnalysisRecord mlAnalysisResponse)
      throws IOException {
    return new RestResponse<>(metricDataAnalysisService.saveAnalysisRecordsML(StateType.DYNA_TRACE, accountId,
        applicationId, stateExecutionId, workflowExecutionId, workflowId, serviceId, analysisMinute, taskId,
        baseLineExecutionId, mlAnalysisResponse));
  }

  @POST
  @Path("/get-tooltip")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Override
  public RestResponse<List<NewRelicMetricHostAnalysisValue>> getTooltip(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workFlowExecutionId") String workFlowExecutionId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.getToolTip(
        stateExecutionId, workFlowExecutionId, analysisMinute, transactionName, metricName));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-metric-template")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  @Override
  public RestResponse<Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(
      @QueryParam("accountId") String accountId) {
    return new RestResponse<>(metricDataAnalysisService.getMetricTemplate(StateType.DYNA_TRACE));
  }
}
