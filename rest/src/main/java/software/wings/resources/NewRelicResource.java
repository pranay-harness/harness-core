package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.ExternalServiceAuth;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.StateType;

import java.io.IOException;
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
public class NewRelicResource {
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
      @QueryParam("applicationId") String applicationId, List<NewRelicMetricDataRecord> metricData) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.saveMetricData(accountId, applicationId, metricData));
  }

  @POST
  @Path("/get-metrics")
  @Timed
  @ExternalServiceAuth
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricDataRecord>> getMetricData(@QueryParam("accountId") String accountId,
      @QueryParam("compareCurrent") boolean compareCurrent, TSRequest request) throws IOException {
    if (compareCurrent) {
      return new RestResponse<>(metricDataAnalysisService.getRecords(StateType.NEW_RELIC,
          request.getWorkflowExecutionId(), request.getStateExecutionId(), request.getWorkflowId(),
          request.getServiceId(), request.getNodes(), request.getAnalysisMinute()));
    } else {
      return new RestResponse<>(metricDataAnalysisService.getPreviousSuccessfulRecords(
          StateType.NEW_RELIC, request.getWorkflowId(), request.getServiceId(), request.getAnalysisMinute()));
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
      @QueryParam("analysisMinute") Integer analysisMinute, TimeSeriesMLAnalysisRecord mlAnalysisResponse)
      throws IOException {
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setWorkflowExecutionId(workflowExecutionId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setAnalysisMinute(analysisMinute);
    for (TimeSeriesMLTxnSummary txnSummary : mlAnalysisResponse.getTransactions().values()) {
      for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
        Iterator<Entry<String, TimeSeriesMLHostSummary>> it = mlMetricSummary.getResults().entrySet().iterator();
        Map<String, TimeSeriesMLHostSummary> timeSeriesMLHostSummaryMap = new HashMap<>();
        while (it.hasNext()) {
          Entry<String, TimeSeriesMLHostSummary> pair = (Map.Entry) it.next();
          timeSeriesMLHostSummaryMap.put(pair.getKey().replaceAll("\\.", "-"), pair.getValue());
        }
        mlMetricSummary.setResults(timeSeriesMLHostSummaryMap);
      }
    }
    return new RestResponse<>(metricDataAnalysisService.saveAnalysisRecordsML(mlAnalysisResponse));
  }
}
