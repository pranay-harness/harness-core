package software.wings.service.intfc;

import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 9/17/18.
 */
public interface MetricDataAnalysisService {
  String RESOURCE_URL = "timeseries";

  List<NewRelicMetricAnalysisRecord> getMetricsAnalysis(
      String appId, String stateExecutionId, String workflowExecutionId);

  boolean isStateValid(String appId, String stateExecutionID);

  String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId);

  List<NewRelicMetricHostAnalysisValue> getToolTip(String stateExecutionId, String workflowExecutionId,
      int analysisMinute, String transactionName, String metricName, String groupName);

  void saveMetricTemplates(String appId, StateType stateType, String stateExecutionId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates);

  void saveMetricGroups(
      String appId, StateType stateType, String stateExecutionId, Map<String, TimeSeriesMlAnalysisGroupInfo> groups);

  Map<String, TimeSeriesMlAnalysisGroupInfo> getMetricGroups(String appId, String stateExecutionId);

  void cleanUpForMetricRetry(String stateExecutionId);

  List<NewRelicMetricAnalysisRecord> getMetricsAnalysisForDemo(
      String appId, String stateExecutionId, String workflowExecutionId);

  boolean saveCustomThreshold(String appId, StateType stateType, String serviceId, String groupName,
      String transactionName, TimeSeriesMetricDefinition metricDefinition);

  TimeSeriesMLTransactionThresholds getCustomThreshold(
      String appId, StateType stateType, String serviceId, String groupName, String transactionName, String metricName);

  boolean deleteCustomThreshold(
      String appId, StateType stateType, String serviceId, String groupName, String transactionName, String metricName);
}
