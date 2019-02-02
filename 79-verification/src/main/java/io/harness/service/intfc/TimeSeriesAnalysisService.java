package io.harness.service.intfc;

import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.MetricAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 9/26/17.
 */
public interface TimeSeriesAnalysisService {
  @ValidationGroups(Create.class)
  boolean saveMetricData(@NotNull String accountId, String appId, String stateExecutionId, String delegateTaskId,
      @Valid List<NewRelicMetricDataRecord> metricData);

  @ValidationGroups(Create.class) void saveAnalysisRecords(@Valid NewRelicMetricAnalysisRecord metricAnalysisRecord);

  @ValidationGroups(Create.class)
  boolean saveAnalysisRecordsML(String accountId, @NotNull StateType stateType, @NotNull String appId,
      @NotNull String stateExecutionId, @NotNull String workflowExecutionId, String groupName,
      @NotNull Integer analysisMinute, @NotNull String taskId, String baseLineExecutionId, String cvConfigId,
      @Valid MetricAnalysisRecord mlAnalysisResponse);

  @ValidationGroups(Create.class) void saveTimeSeriesMLScores(TimeSeriesMLScores scores);

  List<ExperimentalMetricAnalysisRecord> getExperimentalAnalysisRecordsByNaturalKey(
      String appId, String stateExecutionId, String workflowExecutionId);

  List<TimeSeriesMLScores> getTimeSeriesMLScores(String appId, String workflowId, int analysisMinute, int limit);

  List<NewRelicMetricDataRecord> getRecords(String appId, String stateExecutionId, String groupName, Set<String> nodes,
      int analysisMinute, int analysisStartMinute);

  List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      String appId, String workflowExecutionID, String groupName, int analysisMinute, int analysisStartMinute);

  List<String> getLastSuccessfulWorkflowExecutionIds(String appId, String workflowId, String serviceId);

  List<NewRelicMetricAnalysisRecord> getMetricsAnalysis(
      String appId, String stateExecutionId, String workflowExecutionId);

  boolean isStateValid(String appId, String stateExecutionId);

  NewRelicMetricDataRecord getLastHeartBeat(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String metricGroup);

  void bumpCollectionMinuteToProcess(
      String appId, String stateExecutionId, String workflowExecutionId, String groupName, int analysisMinute);

  int getMaxControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName);

  int getMinControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName);

  String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId);

  Map<String, Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(String appId, StateType stateType,
      String stateExecutionId, String serviceId, String cvConfigId, String groupName);

  NewRelicMetricDataRecord getAnalysisMinute(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName);

  Map<String, TimeSeriesMetricDefinition> getMetricTemplates(
      String accountId, StateType stateType, String stateExecutionId, String cvConfigId);

  Map<String, TimeSeriesMlAnalysisGroupInfo> getMetricGroups(String appId, String stateExecutionId);

  void saveMetricTemplates(String appId, StateType stateType, String stateExecutionId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates);

  long getMaxCVCollectionMinute(String appId, String cvConfigId);

  long getLastCVAnalysisMinute(String appId, String cvConfigId);

  List<NewRelicMetricDataRecord> getMetricRecords(StateType stateType, String appId, String serviceId,
      String cvConfigId, int analysisStartMinute, int analysisEndMinute);

  TimeSeriesMLAnalysisRecord getPreviousAnalysis(String appId, String cvConfigId, long dataCollectionMin);

  List<TimeSeriesMLAnalysisRecord> getHistoricalAnalysis(
      String accountId, String appId, String serviceId, String cvConfigId, long analysisMin);

  TimeSeriesAnomaliesRecord getPreviousAnomalies(String appId, String cvConfigId, Map<String, List<String>> metrics);

  List<TimeSeriesCumulativeSums> getCumulativeSumsForRange(
      String appId, String cvConfigId, int startMinute, int endMinute);
}
