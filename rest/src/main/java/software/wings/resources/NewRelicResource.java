package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.ExternalServiceAuth;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLMetricScores;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMLTxnScores;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.MetricAnalysisResource;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 09/05/17.
 */
@Api("newrelic")
@Path("/newrelic")
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class NewRelicResource implements MetricAnalysisResource {
  @Inject private NewRelicService newRelicService;

  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicApplication>> getAllApplications(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId) throws IOException {
    return new RestResponse<>(newRelicService.getApplications(settingId, StateType.NEW_RELIC));
  }

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

  @POST
  @Path("/save-metric-names")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricNames(
      @QueryParam("accountId") final String accountId, NewRelicMetricNames metricNames) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.updateMetricNames(accountId, metricNames));
  }

  @POST
  @Path("/get-metric-names")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<NewRelicMetricNames> getMetricNames(
      @QueryParam("accountId") final String accountId, NewRelicMetricNames metricNames) throws IOException {
    return new RestResponse<>(
        metricDataAnalysisService.getMetricNames(metricNames.getNewRelicAppId(), metricNames.getNewRelicConfigId()));
  }

  @POST
  @Path("/get-metrics")
  @Timed
  @ExternalServiceAuth
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricDataRecord>> getMetricData(@QueryParam("accountId") String accountId,
      @QueryParam("workflowExecutionId") String workFlowExecutionId,
      @QueryParam("compareCurrent") boolean compareCurrent, TSRequest request) throws IOException {
    if (compareCurrent) {
      return new RestResponse<>(metricDataAnalysisService.getRecords(StateType.NEW_RELIC,
          request.getWorkflowExecutionId(), request.getStateExecutionId(), request.getWorkflowId(),
          request.getServiceId(), request.getNodes(), request.getAnalysisMinute(), request.getAnalysisStartMinute()));
    } else {
      if (workFlowExecutionId == null || workFlowExecutionId.equals("-1")) {
        return new RestResponse<>(new ArrayList<>());
      }

      return new RestResponse<>(metricDataAnalysisService.getPreviousSuccessfulRecords(StateType.NEW_RELIC,
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
    return new RestResponse<>(
        metricDataAnalysisService.getMetricsAnalysis(StateType.NEW_RELIC, stateExecutionId, workflowExecutionId));
  }

  @POST
  @Path("/save-analysis")
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<Boolean> saveMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("workflowId") final String workflowId, @QueryParam("analysisMinute") Integer analysisMinute,
      TimeSeriesMLAnalysisRecord mlAnalysisResponse) throws IOException {
    mlAnalysisResponse.setStateType(StateType.NEW_RELIC);
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setWorkflowExecutionId(workflowExecutionId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setAnalysisMinute(analysisMinute);

    TimeSeriesMLScores timeSeriesMLScores = TimeSeriesMLScores.builder()
                                                .applicationId(applicationId)
                                                .stateExecutionId(stateExecutionId)
                                                .workflowExecutionId(workflowExecutionId)
                                                .workflowId(workflowId)
                                                .analysisMinute(analysisMinute)
                                                .stateType(StateType.NEW_RELIC)
                                                .scoresMap(new HashMap<>())
                                                .build();

    int txnId = 0;
    int metricId;
    for (TimeSeriesMLTxnSummary txnSummary : mlAnalysisResponse.getTransactions().values()) {
      TimeSeriesMLTxnScores txnScores =
          TimeSeriesMLTxnScores.builder().transactionName(txnSummary.getTxn_name()).scoresMap(new HashMap<>()).build();
      timeSeriesMLScores.getScoresMap().put(String.valueOf(txnId), txnScores);

      metricId = 0;
      for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
        if (mlMetricSummary.getResults() != null) {
          TimeSeriesMLMetricScores mlMetricScores = TimeSeriesMLMetricScores.builder()
                                                        .metricName(mlMetricSummary.getMetric_name())
                                                        .scores(new ArrayList<>())
                                                        .build();
          txnScores.getScoresMap().put(String.valueOf(metricId), mlMetricScores);

          Iterator<Entry<String, TimeSeriesMLHostSummary>> it = mlMetricSummary.getResults().entrySet().iterator();
          Map<String, TimeSeriesMLHostSummary> timeSeriesMLHostSummaryMap = new HashMap<>();
          while (it.hasNext()) {
            Entry<String, TimeSeriesMLHostSummary> pair = it.next();
            timeSeriesMLHostSummaryMap.put(pair.getKey().replaceAll("\\.", "-"), pair.getValue());
            mlMetricScores.getScores().add(pair.getValue().getScore());
          }
          mlMetricSummary.setResults(timeSeriesMLHostSummaryMap);
          ++metricId;
        }
      }
      ++txnId;
    }

    metricDataAnalysisService.saveTimeSeriesMLScores(timeSeriesMLScores);

    return new RestResponse<>(metricDataAnalysisService.saveAnalysisRecordsML(mlAnalysisResponse));
  }

  @POST
  @Path("/get-scores")
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
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
  @ExternalServiceAuth
  public RestResponse<List<NewRelicMetricHostAnalysisValue>> getTooltip(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workFlowExecutionId") String workFlowExecutionId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.getToolTip(
        stateExecutionId, workFlowExecutionId, analysisMinute, transactionName, metricName));
  }

  @POST
  @Path("/get-metric-template")
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(
      @QueryParam("accountId") String accountId) {
    return new RestResponse<>(metricDataAnalysisService.getMetricTemplate(StateType.NEW_RELIC));
  }
}
