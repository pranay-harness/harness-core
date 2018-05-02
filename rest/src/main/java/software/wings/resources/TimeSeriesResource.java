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
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
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
      @QueryParam("stateType") StateType stateType, @QueryParam("workflowExecutionId") String workFlowExecutionId,
      @QueryParam("compareCurrent") boolean compareCurrent, TSRequest request) throws IOException {
    if (compareCurrent) {
      return new RestResponse<>(metricDataAnalysisService.getRecords(stateType, request.getWorkflowExecutionId(),
          request.getStateExecutionId(), request.getWorkflowId(), request.getServiceId(), request.getNodes(),
          request.getAnalysisMinute(), request.getAnalysisStartMinute()));
    } else {
      if (workFlowExecutionId == null || workFlowExecutionId.equals("-1")) {
        return new RestResponse<>(new ArrayList<>());
      }

      return new RestResponse<>(metricDataAnalysisService.getPreviousSuccessfulRecords(stateType,
          request.getWorkflowId(), workFlowExecutionId, request.getServiceId(), request.getAnalysisMinute(),
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
      @QueryParam("accountId") final String accountId) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.getMetricsAnalysis(stateExecutionId, workflowExecutionId));
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
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("taskId") String taskId,
      @QueryParam("baseLineExecutionId") String baseLineExecutionId, TimeSeriesMLAnalysisRecord mlAnalysisResponse)
      throws IOException {
    return new RestResponse<>(metricDataAnalysisService.saveAnalysisRecordsML(stateType, accountId, applicationId,
        stateExecutionId, workflowExecutionId, workflowId, serviceId, analysisMinute, taskId, baseLineExecutionId,
        mlAnalysisResponse));
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
  @LearningEngineAuth
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
  public RestResponse<Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(
      @QueryParam("accountId") String accountId, @QueryParam("stateType") StateType stateType,
      @QueryParam("stateExecutionId") String stateExecutionId) {
    return new RestResponse<>(metricDataAnalysisService.getMetricTemplate(stateType, stateExecutionId));
  }
}
