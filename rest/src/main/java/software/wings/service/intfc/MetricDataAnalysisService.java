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
  String RESOURCE_URL = "timeseries";
  @ValidationGroups(Create.class)
  boolean saveMetricData(@NotNull String accountId, String appId, String stateExecutionId, String delegateTaskId,
      @Valid List<NewRelicMetricDataRecord> metricData) throws IOException;

  @ValidationGroups(Create.class) boolean saveAnalysisRecords(@Valid NewRelicMetricAnalysisRecord metricAnalysisRecord);

  @ValidationGroups(Create.class)
  boolean saveAnalysisRecordsML(@NotNull StateType stateType, @NotNull String accountId, @NotNull String appId,
      @NotNull String stateExecutionId, @NotNull String workflowExecutionId, @NotNull String workflowId,
      @NotNull String serviceId, @NotNull Integer analysisMinute, @NotNull String taskId, String baseLineExecutionId,
      @Valid TimeSeriesMLAnalysisRecord mlAnalysisResponse);

  @ValidationGroups(Create.class) void saveTimeSeriesMLScores(TimeSeriesMLScores scores);

  List<TimeSeriesMLScores> getTimeSeriesMLScores(String appId, String workflowId, int analysisMinute, int limit);

  List<NewRelicMetricDataRecord> getRecords(StateType stateType, String appId, String workflowExecutionId,
      String stateExecutionId, String workflowId, String serviceId, Set<String> nodes, int analysisMinute,
      int analysisStartMinute);

  List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(StateType stateType, String appId, String workflowId,
      String serviceId, int analysisMinute, int analysisStartMinute);

  List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(StateType stateType, String appId, String workflowId,
      String workflowExecutionID, String serviceId, int analysisMinute, int analysisStartMinute);

  List<String> getLastSuccessfulWorkflowExecutionIds(String workflowId);

  NewRelicMetricAnalysisRecord getMetricsAnalysis(String stateExecutionId, String workflowExecutionId);

  boolean isStateValid(String appId, String stateExecutionID);

  NewRelicMetricDataRecord getLastHeartBeat(
      StateType stateType, String appId, String stateExecutionId, String workflowExecutionId, String serviceId);

  void bumpCollectionMinuteToProcess(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, int analysisMinute);

  int getMaxControlMinuteWithData(
      StateType stateType, String appId, String serviceId, String workflowId, String workflowExecutionId);

  int getMinControlMinuteWithData(
      StateType stateType, String appId, String serviceId, String workflowId, String workflowExecutionId);

  String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId);

  List<NewRelicMetricHostAnalysisValue> getToolTip(String stateExecutionId, String workflowExecutionId,
      int analysisMinute, String transactionName, String metricName);

  Map<String, TimeSeriesMetricDefinition> getMetricTemplate(StateType stateType, String stateExecutionId);

  NewRelicMetricDataRecord getAnalysisMinute(
      StateType stateType, String appId, String stateExecutionId, String workflowExecutionId, String serviceId);

  void saveMetricTemplates(
      StateType stateType, String stateExecutionId, Map<String, TimeSeriesMetricDefinition> metricTemplates);

  Map<String, TimeSeriesMetricDefinition> getMetricTemplates(StateType stateType, String stateExecutionId);

  void cleanUpForMetricRetry(String stateExecutionId);
}
