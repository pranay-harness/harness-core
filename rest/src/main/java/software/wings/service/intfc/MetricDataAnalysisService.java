package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;
import software.wings.utils.validation.Create;

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
  @ValidationGroups(Create.class)
  boolean saveMetricData(@NotNull String accountId, String applicationId, String stateExecutionId,
      String delegateTaskId, @Valid List<NewRelicMetricDataRecord> metricData) throws IOException;

  @ValidationGroups(Create.class) boolean saveAnalysisRecords(@Valid NewRelicMetricAnalysisRecord metricAnalysisRecord);

  @ValidationGroups(Create.class)
  boolean saveAnalysisRecordsML(@Valid TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord);

  @ValidationGroups(Create.class) void saveTimeSeriesMLScores(TimeSeriesMLScores scores);

  List<TimeSeriesMLScores> getTimeSeriesMLScores(
      String applicationId, String workflowId, int analysisMinute, int limit);

  List<NewRelicMetricDataRecord> getRecords(StateType stateType, String workflowExecutionId, String stateExecutionId,
      String workflowId, String serviceId, Set<String> nodes, int analysisMinute);

  List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      StateType stateType, String workflowId, String serviceId, int analysisMinute);

  List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      StateType stateType, String workflowId, String workflowExecutionID, String serviceId, int analysisMinute);

  List<String> getLastSuccessfulWorkflowExecutionIds(String workflowId);

  NewRelicMetricAnalysisRecord getMetricsAnalysis(
      StateType stateType, String stateExecutionId, String workflowExecutionId);

  boolean isStateValid(String appdId, String stateExecutionID);

  int getCollectionMinuteToProcess(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId);

  void bumpCollectionMinuteToProcess(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId, int analysisMinute);

  int getMaxControlMinuteWithData(StateType stateType, String serviceId, String workflowId, String workflowExecutionId);

  int getMinControlMinuteWithData(StateType stateType, String serviceId, String workflowId, String workflowExecutionId);

  String getLastSuccessfulWorkflowExecutionIdWithData(StateType stateType, String workflowId, String serviceId);

  List<NewRelicMetricHostAnalysisValue> getToolTip(String stateExecutionId, String workflowExecutionId,
      int analysisMinute, String transactionName, String metricName);

  Map<String, TimeSeriesMetricDefinition> getMetricTemplate(StateType stateType);
}
