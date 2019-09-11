package io.harness.service.intfc;

import io.harness.beans.SortOrder.OrderType;
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
import software.wings.service.impl.analysis.Version;
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
      @Valid MetricAnalysisRecord mlAnalysisResponse, String tag);

  @ValidationGroups(Create.class) void saveTimeSeriesMLScores(TimeSeriesMLScores scores);

  List<ExperimentalMetricAnalysisRecord> getExperimentalAnalysisRecordsByNaturalKey(
      String appId, String stateExecutionId, String workflowExecutionId);

  List<TimeSeriesMLScores> getTimeSeriesMLScores(String appId, String workflowId, int analysisMinute, int limit);

  Set<NewRelicMetricDataRecord> getRecords(String appId, String stateExecutionId, String groupName, Set<String> nodes,
      int analysisMinute, int analysisStartMinute);

  Set<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      String appId, String workflowExecutionID, String groupName, int analysisMinute, int analysisStartMinute);

  List<String> getLastSuccessfulWorkflowExecutionIds(String appId, String workflowId, String serviceId);

  NewRelicMetricDataRecord getHeartBeat(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String metricGroup, OrderType orderType);

  void bumpCollectionMinuteToProcess(String appId, String stateExecutionId, String workflowExecutionId,
      String groupName, int analysisMinute, String accountId);

  int getMaxControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName);

  int getMinControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName);

  String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId);

  Map<String, Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(String appId, StateType stateType,
      String stateExecutionId, String serviceId, String cvConfigId, String groupName);

  Map<String, Map<String, TimeSeriesMetricDefinition>> getMetricTemplateWithCategorizedThresholds(String appId,
      StateType stateType, String stateExecutionId, String serviceId, String cvConfigId, String groupName,
      Version version);

  NewRelicMetricDataRecord getAnalysisMinute(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName);

  Map<String, TimeSeriesMetricDefinition> getMetricTemplates(
      String accountId, StateType stateType, String stateExecutionId, String cvConfigId);

  Map<String, TimeSeriesMlAnalysisGroupInfo> getMetricGroups(String appId, String stateExecutionId);

  void saveMetricTemplates(String appId, StateType stateType, String stateExecutionId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates);

  long getMaxCVCollectionMinute(String appId, String cvConfigId, String accountId);

  long getLastCVAnalysisMinute(String appId, String cvConfigId);

  Set<NewRelicMetricDataRecord> getMetricRecords(
      String cvConfigId, int analysisStartMinute, int analysisEndMinute, String tag, String accountId);

  TimeSeriesMLAnalysisRecord getPreviousAnalysis(String appId, String cvConfigId, long dataCollectionMin, String tag);

  List<TimeSeriesMLAnalysisRecord> getHistoricalAnalysis(
      String accountId, String appId, String serviceId, String cvConfigId, long analysisMin, String tag);

  TimeSeriesAnomaliesRecord getPreviousAnomalies(
      String appId, String cvConfigId, Map<String, List<String>> metrics, String tag);

  Set<TimeSeriesCumulativeSums> getCumulativeSumsForRange(
      String appId, String cvConfigId, int startMinute, int endMinute, String tag);

  long getLastDataCollectedMinute(String appId, String stateExecutionId, StateType stateType);
}
