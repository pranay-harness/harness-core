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
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.MetricAnalysisResource;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 09/05/17.
 *
 * We are versioning the Learning engine apis using the Accept Header. Clients can
 * request a specific version of the api by sending the version in the Accept header
 * as follows:
 *
 * Accept: application/v1+json, application/json
 *
 * This requests for version v1 of the api, and if that is not available it asks the server
 * to send the default api response. This is indicated by appending application/json to the
 * Accept header.
 *
 * Created 2 test apis test1 and test2 to show how to create versioned apis. Note that test1 serves
 * v1 and test2 serves v2 of the same api /test. Note that only the latest version v2 has 2 entries
 * in produces - one with a version and another with just application/json. The application/json
 * without the api version makes it the default implementation for /test api. So clients with no Accept
 * header and clients requesting just application/json will be served by the default api.
 *
 * IMPORTANT: Make sure that the latest version that is added is made the default api. There should
 * only be one default api.
 *
 *
 * For now, only apis used by the learning engine are versioned.
 *
 */
@Api("newrelic")
@Path("/newrelic")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class NewRelicResource implements MetricAnalysisResource {
  @Inject private NewRelicService newRelicService;

  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @Inject private LearningEngineService learningEngineService;

  @Produces({"application/json", "application/v2+json"})
  @GET
  @Path("/test")
  @Timed
  @ExceptionMetered
  public RestResponse<String> test2(@QueryParam("accountId") String accountId) throws IOException {
    Map<String, Object> metaData = new HashMap<>();
    metaData.put("version", "v2");
    return RestResponse.Builder.aRestResponse().withResource("v2").withMetaData(metaData).build();
  }

  @Produces("application/v1+json")
  @GET
  @Path("/test")
  @Timed
  @ExceptionMetered
  public RestResponse<String> test1(@QueryParam("accountId") String accountId) throws IOException {
    return new RestResponse<>("v1");
  }

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

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/get-metrics")
  @Timed
  @LearningEngineAuth
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

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/save-analysis")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> saveMLAnalysisRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("workflowId") final String workflowId, @QueryParam("serviceId") final String serviceId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("taskId") String taskId,
      @QueryParam("baseLineExecutionId") String baseLineExecutionId, TimeSeriesMLAnalysisRecord mlAnalysisResponse)
      throws IOException {
    return new RestResponse<>(metricDataAnalysisService.saveAnalysisRecordsML(StateType.NEW_RELIC, accountId,
        applicationId, stateExecutionId, workflowExecutionId, workflowId, serviceId, analysisMinute, taskId,
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
      @QueryParam("accountId") String accountId) {
    return new RestResponse<>(metricDataAnalysisService.getMetricTemplate(StateType.NEW_RELIC));
  }
}
