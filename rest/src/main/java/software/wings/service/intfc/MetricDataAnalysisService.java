package software.wings.service.intfc;

import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.MetricAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 9/26/17.
 */
public interface MetricDataAnalysisService {
  String RESOURCE_URL = "timeseries";

  @ValidationGroups(Create.class)
  boolean saveMetricData(@NotNull String accountId, String appId, String stateExecutionId, String delegateTaskId,
      @Valid List<NewRelicMetricDataRecord> metricData) throws IOException;

  @ValidationGroups(Create.class) boolean saveAnalysisRecords(@Valid NewRelicMetricAnalysisRecord metricAnalysisRecord);

  @ValidationGroups(Create.class)
  boolean saveAnalysisRecordsML(@NotNull StateType stateType, @NotNull String accountId, @NotNull String appId,
      @NotNull String stateExecutionId, @NotNull String workflowExecutionId, @NotNull String workflowId,
      @NotNull String serviceId, String groupName, @NotNull Integer analysisMinute, @NotNull String taskId,
      String baseLineExecutionId, @Valid MetricAnalysisRecord mlAnalysisResponse);

  @ValidationGroups(Create.class) void saveTimeSeriesMLScores(TimeSeriesMLScores scores);

  List<ExperimentalMetricAnalysisRecord> getExperimentalAnalysisRecordsByNaturalKey(
      String appId, String stateExecutionId, String workflowExecutionId);

  List<TimeSeriesMLScores> getTimeSeriesMLScores(String appId, String workflowId, int analysisMinute, int limit);

  List<NewRelicMetricDataRecord> getRecords(StateType stateType, String appId, String workflowExecutionId,
      String stateExecutionId, String workflowId, String serviceId, String groupName, Set<String> nodes,
      int analysisMinute, int analysisStartMinute);

  List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(StateType stateType, String appId, String workflowId,
      String workflowExecutionID, String serviceId, String groupName, int analysisMinute, int analysisStartMinute);

  List<String> getLastSuccessfulWorkflowExecutionIds(String appId, String workflowId, String serviceId);

  List<NewRelicMetricAnalysisRecord> getMetricsAnalysis(
      String appId, String stateExecutionId, String workflowExecutionId);

  boolean isStateValid(String appId, String stateExecutionID);

  NewRelicMetricDataRecord getLastHeartBeat(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String metricGroup);

  void bumpCollectionMinuteToProcess(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName, int analysisMinute);

  int getMaxControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName);

  int getMinControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName);

  String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId);

  List<NewRelicMetricHostAnalysisValue> getToolTip(String stateExecutionId, String workflowExecutionId,
      int analysisMinute, String transactionName, String metricName, String groupName);

  Map<String, Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(
      String appId, StateType stateType, String stateExecutionId, String serviceId, String groupName);
  Map<String, Map<String, TimeSeriesMetricDefinition>> getCustomMetricTemplates(
      String appId, StateType stateType, String serviceId, String groupName);

  NewRelicMetricDataRecord getAnalysisMinute(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName);

  void saveMetricTemplates(String appId, StateType stateType, String stateExecutionId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates);

  Map<String, TimeSeriesMetricDefinition> getMetricTemplates(
      String accountId, StateType stateType, String stateExecutionId);

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
