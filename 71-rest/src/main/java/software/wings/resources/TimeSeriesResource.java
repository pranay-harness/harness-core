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
import software.wings.security.annotations.Scope;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
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

  @GET
  @Path("/generate-metrics-appdynamics")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicMetricAnalysisRecord>> getMetricsAnalysisAppdynamics(
      @QueryParam("stateExecutionId") final String stateExecutionId,
      @QueryParam("workflowExecutionId") final String workflowExecutionId,
      @QueryParam("accountId") final String accountId, @QueryParam("appId") final String appId) throws IOException {
    if (featureFlagService.isEnabledReloadCache(FeatureName.CV_DEMO, accountId)) {
      return new RestResponse<>(Lists.newArrayList(
          metricDataAnalysisService.getMetricsAnalysisForDemo(appId, stateExecutionId, workflowExecutionId).get(0)));
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
      @QueryParam("workFlowExecutionId") String workFlowExecutionId,
      @QueryParam("analysisMinute") Integer analysisMinute, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName, @QueryParam("groupName") String groupName) throws IOException {
    return new RestResponse<>(metricDataAnalysisService.getToolTip(
        stateExecutionId, workFlowExecutionId, analysisMinute, transactionName, metricName, groupName));
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
      @QueryParam("transactionName") String transactionName, TimeSeriesMetricDefinition timeSeriesMetricDefinition) {
    return new RestResponse<>(metricDataAnalysisService.saveCustomThreshold(
        appId, stateType, serviceId, groupName, transactionName, timeSeriesMetricDefinition));
  }

  @DELETE
  @Path("/threshold")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteCustomThreshold(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName,
      @QueryParam("transactionName") String transactionName, @QueryParam("metricName") String metricName) {
    return new RestResponse<>(metricDataAnalysisService.deleteCustomThreshold(
        appId, stateType, serviceId, groupName, transactionName, metricName));
  }
}
