/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import io.harness.beans.ExecutionStatus;

import software.wings.metrics.ThresholdComparisonType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.DeploymentTimeSeriesAnalysis;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.sm.StateType;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by rsingh on 9/17/18.
 */
public interface MetricDataAnalysisService {
  String RESOURCE_URL = "timeseries";

  Set<NewRelicMetricAnalysisRecord> getMetricsAnalysis(
      String appId, String stateExecutionId, String workflowExecutionId);

  NewRelicMetricAnalysisRecord getLatestLocalAnalysisRecord(String stateExecutionId);

  DeploymentTimeSeriesAnalysis getMetricsAnalysis(
      String stateExecutionId, Optional<Integer> offset, Optional<Integer> pageSize, boolean isDemoPath);

  DeploymentTimeSeriesAnalysis getMetricsAnalysisForDemo(
      String stateExecutionId, Optional<Integer> offset, Optional<Integer> pageSize);

  boolean isStateValid(String appId, String stateExecutionID);

  String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId, String infraMappingIds, String envId);

  List<NewRelicMetricHostAnalysisValue> getToolTip(
      String stateExecutionId, int analysisMinute, String transactionName, String metricName, String groupName);

  List<NewRelicMetricHostAnalysisValue> getToolTipForDemo(String stateExecutionId, String workflowExecutionId,
      int analysisMinute, String transactionName, String metricName, String groupName);

  void saveMetricTemplates(String appId, StateType stateType, String stateExecutionId, String cvConfigId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates);

  void saveMetricGroups(
      String appId, StateType stateType, String stateExecutionId, Map<String, TimeSeriesMlAnalysisGroupInfo> groups);

  Map<String, TimeSeriesMlAnalysisGroupInfo> getMetricGroups(String appId, String stateExecutionId);

  void cleanUpForMetricRetry(String stateExecutionId);

  Set<NewRelicMetricAnalysisRecord> getMetricsAnalysisForDemo(
      String appId, String stateExecutionId, String workflowExecutionId);

  TimeSeriesMLTransactionThresholds getCustomThreshold(String appId, StateType stateType, String serviceId,
      String cvConfigId, String groupName, String transactionName, String metricName, String customThresholdRefId)
      throws UnsupportedEncodingException;

  List<TimeSeriesMLTransactionThresholds> getCustomThreshold(String fieldName, String fieldValue);
  List<TimeSeriesMLTransactionThresholds> getCustomThreshold(String customThresholdRefId);

  boolean saveCustomThreshold(String accountId, String appId, StateType stateType, String serviceId, String cvConfigId,
      String transactionName, String groupName, TimeSeriesMetricDefinition metricDefinition,
      String customThresholdRefId);
  boolean saveCustomThreshold(String serviceId, String cvConfigId, List<TimeSeriesMLTransactionThresholds> thresholds);

  boolean deleteCustomThreshold(List<String> thresholdIdsToBeDeleted);

  boolean deleteCustomThreshold(String appId, StateType stateType, String serviceId, String cvConfigId,
      String groupName, String transactionName, String metricName, ThresholdComparisonType thresholdComparisonType,
      String customThresholdRefId) throws UnsupportedEncodingException;

  boolean bulkDeleteCustomThreshold(String customThresholdRefId);
  void saveRawDataToGoogleDataStore(
      String accountId, String stateExecutionId, ExecutionStatus executionStatus, String serviceId);
}
