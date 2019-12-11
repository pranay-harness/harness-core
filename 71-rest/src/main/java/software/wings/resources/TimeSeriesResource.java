package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.APMFetchConfig;
import software.wings.beans.FeatureName;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.DeploymentTimeSeriesAnalysis;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.DELETE;
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
  @Inject private CVConfigurationService cvConfigurationService;

  @GET
  @Path("/metric-analysis")
  @Timed
  @ExceptionMetered
  public RestResponse<DeploymentTimeSeriesAnalysis> getMetricsAnalysis(
      @QueryParam("stateExecutionId") final String stateExecutionId, @QueryParam("accountId") final String accountId,
      @QueryParam("offset") final Integer offset, @QueryParam("pageSize") final Integer pageSize) {
    if (featureFlagService.isEnabledReloadCache(FeatureName.CV_DEMO, accountId)) {
      return new RestResponse<>(metricDataAnalysisService.getMetricsAnalysisForDemo(
          stateExecutionId, Optional.ofNullable(offset), Optional.ofNullable(pageSize)));
    }
    return new RestResponse<>(metricDataAnalysisService.getMetricsAnalysis(
        stateExecutionId, Optional.ofNullable(offset), Optional.ofNullable(pageSize)));
  }

  @GET
  @Path("/generate-metrics-appdynamics")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<NewRelicMetricAnalysisRecord>> getMetricsAnalysisAppdynamics(
      @QueryParam("stateExecutionId") final String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("accountId") final String accountId, @QueryParam("appId") final String appId) {
    if (featureFlagService.isEnabledReloadCache(FeatureName.CV_DEMO, accountId)) {
      return new RestResponse<>(
          metricDataAnalysisService.getMetricsAnalysisForDemo(appId, stateExecutionId, workflowExecutionId));
    }
    return new RestResponse<>(
        metricDataAnalysisService.getMetricsAnalysis(appId, stateExecutionId, workflowExecutionId));
  }

  @GET
  @Path("/get-tooltip")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricHostAnalysisValue>> getTooltip(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId,
      @QueryParam("workflowExecutionId") String workflowExecutionId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName, @QueryParam("groupName") String groupName) {
    if (featureFlagService.isEnabledReloadCache(FeatureName.CV_DEMO, accountId)) {
      return new RestResponse<>(metricDataAnalysisService.getToolTipForDemo(
          stateExecutionId, workflowExecutionId, analysisMinute, transactionName, metricName, groupName));
    }
    return new RestResponse<>(
        metricDataAnalysisService.getToolTip(stateExecutionId, analysisMinute, transactionName, metricName, groupName));
  }

  @POST
  @Path("/fetch")
  @Timed
  @ExceptionMetered
  public RestResponse<String> fetch(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String serverConfigId, APMFetchConfig fetchConfig) {
    return new RestResponse<>(newRelicService.fetch(accountId, serverConfigId, fetchConfig));
  }

  @POST
  @Path("/threshold")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveCustomThreshold(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName,
      @QueryParam("transactionName") String transactionName, @QueryParam("cvConfigId") String cvConfigId,
      TimeSeriesMetricDefinition timeSeriesMetricDefinition) {
    return new RestResponse<>(metricDataAnalysisService.saveCustomThreshold(
        appId, stateType, serviceId, cvConfigId, transactionName, groupName, timeSeriesMetricDefinition));
  }

  @GET
  @Path("/threshold")
  @Timed
  @ExceptionMetered
  public RestResponse<TimeSeriesMLTransactionThresholds> getCustomThreshold(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName,
      @QueryParam("transactionName") String transactionName, @QueryParam("metricName") String metricName,
      @QueryParam("cvConfigId") String cvConfigId) throws UnsupportedEncodingException {
    return new RestResponse<>(metricDataAnalysisService.getCustomThreshold(
        appId, stateType, serviceId, cvConfigId, groupName, transactionName, metricName));
  }

  @GET
  @Path("/thresholds-for-cvconfig")
  @Timed
  @ExceptionMetered
  public RestResponse<List<TimeSeriesMLTransactionThresholds>> getCustomThresholdForCVConfig(
      @QueryParam("accountId") String accountId, @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(
        metricDataAnalysisService.getCustomThreshold(TimeSeriesMLTransactionThresholdKeys.cvConfigId, cvConfigId));
  }

  @GET
  @Path("/thresholds-for-workflow")
  @Timed
  @ExceptionMetered
  public RestResponse<List<TimeSeriesMLTransactionThresholds>> getCustomThresholdForWorkflow(
      @QueryParam("accountId") String accountId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(
        metricDataAnalysisService.getCustomThreshold(TimeSeriesMLTransactionThresholdKeys.serviceId, serviceId));
  }

  @DELETE
  @Path("/threshold")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteCustomThreshold(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName) throws UnsupportedEncodingException {
    return new RestResponse<>(metricDataAnalysisService.deleteCustomThreshold(
        appId, stateType, serviceId, cvConfigId, groupName, transactionName, metricName));
  }

  @GET
  @Path("/txn-metric-for-cvconfig")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getTxnMetricPairsForAPMCVConfig(
      @QueryParam("accountId") String accountId, @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(cvConfigurationService.getTxnMetricPairsForAPMCVConfig(cvConfigId));
  }
}
