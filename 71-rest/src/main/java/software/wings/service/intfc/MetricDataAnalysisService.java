package software.wings.service.intfc;

import io.harness.beans.ExecutionStatus;
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

  DeploymentTimeSeriesAnalysis getMetricsAnalysis(
      String stateExecutionId, Optional<Integer> offset, Optional<Integer> pageSize);

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
      String cvConfigId, String groupName, String transactionName, String metricName)
      throws UnsupportedEncodingException;

  List<TimeSeriesMLTransactionThresholds> getCustomThreshold(String fieldName, String fieldValue);

  boolean saveCustomThreshold(String appId, StateType stateType, String serviceId, String cvConfigId,
      String transactionName, String groupName, TimeSeriesMetricDefinition metricDefinition);
  boolean saveCustomThreshold(String serviceId, String cvConfigId, List<TimeSeriesMLTransactionThresholds> thresholds);

  boolean deleteCustomThreshold(String appId, StateType stateType, String serviceId, String cvConfigId,
      String groupName, String transactionName, String metricName) throws UnsupportedEncodingException;

  void saveRawDataToGoogleDataStore(
      String accountId, String stateExecutionId, ExecutionStatus executionStatus, String serviceId);
}
