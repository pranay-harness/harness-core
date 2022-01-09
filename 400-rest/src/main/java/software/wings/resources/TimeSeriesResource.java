/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.rest.RestResponse;

import software.wings.APMFetchConfig;
import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.DeploymentTimeSeriesAnalysis;
import software.wings.service.impl.analysis.TimeSeriesKeyTransactions;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
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
        stateExecutionId, Optional.ofNullable(offset), Optional.ofNullable(pageSize), false));
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
      @QueryParam("customThresholdRefId") String customThresholdRefId,
      TimeSeriesMetricDefinition timeSeriesMetricDefinition) {
    return new RestResponse<>(metricDataAnalysisService.saveCustomThreshold(accountId, appId, stateType, serviceId,
        cvConfigId, transactionName, groupName, timeSeriesMetricDefinition, customThresholdRefId));
  }

  @POST
  @Path("/custom-threshold-list")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveCustomThresholdList(@QueryParam("accountId") String accountId,
      @QueryParam("serviceId") String serviceId, @QueryParam("cvConfigId") String cvConfigId,
      List<TimeSeriesMLTransactionThresholds> timeSeriesMLTransactionThresholds) {
    return new RestResponse<>(
        metricDataAnalysisService.saveCustomThreshold(serviceId, cvConfigId, timeSeriesMLTransactionThresholds));
  }

  @GET
  @Path("/threshold")
  @Timed
  @ExceptionMetered
  public RestResponse<TimeSeriesMLTransactionThresholds> getCustomThreshold(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName,
      @QueryParam("transactionName") String transactionName, @QueryParam("metricName") String metricName,
      @QueryParam("customThresholdRefId") String customThresholdRefId, @QueryParam("cvConfigId") String cvConfigId)
      throws UnsupportedEncodingException {
    return new RestResponse<>(metricDataAnalysisService.getCustomThreshold(
        appId, stateType, serviceId, cvConfigId, groupName, transactionName, metricName, customThresholdRefId));
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

  @GET
  @Path("/thresholds")
  @Timed
  @ExceptionMetered
  public RestResponse<List<TimeSeriesMLTransactionThresholds>> getCustomThresholdWithRefId(
      @QueryParam("accountId") String accountId, @QueryParam("customThresholdRefId") String customThresholdRefId) {
    return new RestResponse<>(metricDataAnalysisService.getCustomThreshold(customThresholdRefId));
  }

  @DELETE
  @Path("/threshold-by-id")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteCustomThreshold(
      @QueryParam("accountId") String accountId, @QueryParam("thresholdIdList") List<String> thresholdsToBeDeleted) {
    return new RestResponse<>(metricDataAnalysisService.deleteCustomThreshold(thresholdsToBeDeleted));
  }

  @DELETE
  @Path("/threshold")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteCustomThreshold(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("stateType") StateType stateType,
      @QueryParam("serviceId") String serviceId, @QueryParam("groupName") String groupName,
      @QueryParam("cvConfigId") String cvConfigId, @QueryParam("transactionName") String transactionName,
      @QueryParam("metricName") String metricName,
      @QueryParam("comparisonType") ThresholdComparisonType thresholdComparisonType,
      @QueryParam("customThresholdRefId") String customThresholdRefId) throws UnsupportedEncodingException {
    return new RestResponse<>(metricDataAnalysisService.deleteCustomThreshold(appId, stateType, serviceId, cvConfigId,
        groupName, transactionName, metricName, thresholdComparisonType, customThresholdRefId));
  }

  @DELETE
  @Path("/bulk-threshold")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> bulkDeleteCustomThresholds(
      @QueryParam("accountId") String accountId, @QueryParam("customThresholdRefId") String customThresholdRefId) {
    return new RestResponse<>(metricDataAnalysisService.bulkDeleteCustomThreshold(customThresholdRefId));
  }

  @GET
  @Path("/txn-metric-for-cvconfig")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getTxnMetricPairsForAPMCVConfig(
      @QueryParam("accountId") String accountId, @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(cvConfigurationService.getTxnMetricPairsForAPMCVConfig(cvConfigId));
  }

  @POST
  @Path("/key-transactions")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveKeyTransactions(@QueryParam("accountId") String accountId,
      @QueryParam("cvConfigId") String cvConfigId, List<String> transactionNames) {
    return new RestResponse<>(
        cvConfigurationService.saveKeyTransactionsForCVConfiguration(accountId, cvConfigId, transactionNames));
  }

  @POST
  @Path("/add-to-key-transactions")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> addToKeyTransactions(@QueryParam("accountId") String accountId,
      @QueryParam("cvConfigId") String cvConfigId, List<String> transactionName) {
    return new RestResponse<>(
        cvConfigurationService.addToKeyTransactionsForCVConfiguration(accountId, cvConfigId, transactionName));
  }

  @POST
  @Path("/remove-from-key-transactions")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> removeFromKeyTransactions(@QueryParam("accountId") String accountId,
      @QueryParam("cvConfigId") String cvConfigId, List<String> transactionName) {
    return new RestResponse<>(
        cvConfigurationService.removeFromKeyTransactionsForCVConfiguration(cvConfigId, transactionName));
  }

  @GET
  @Path("/key-transactions")
  @Timed
  @ExceptionMetered
  public RestResponse<TimeSeriesKeyTransactions> getKeyTransactions(
      @QueryParam("accountId") String accountId, @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(cvConfigurationService.getKeyTransactionsForCVConfiguration(cvConfigId));
  }
}
