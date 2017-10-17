package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;
import software.wings.utils.validation.Create;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 9/26/17.
 */
public interface MetricDataAnalysisService {
  @ValidationGroups(Create.class)
  boolean saveMetricData(@NotNull String accountId, String applicationId,
      @Valid List<NewRelicMetricDataRecord> metricData) throws IOException;

  @ValidationGroups(Create.class) boolean saveAnalysisRecords(@Valid NewRelicMetricAnalysisRecord metricAnalysisRecord);

  @ValidationGroups(Create.class)
  boolean saveAnalysisRecordsML(@Valid TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord);

  List<NewRelicMetricDataRecord> getRecords(StateType stateType, String workflowExecutionId, String stateExecutionId,
      String workflowId, String serviceId, Set<String> nodes, int analysisMinute);

  List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      StateType stateType, String workflowId, String serviceId, int analysisMinute);

  NewRelicMetricAnalysisRecord getMetricsAnalysis(
      StateType stateType, String stateExecutionId, String workflowExecutionId);

  boolean isStateValid(String appdId, String stateExecutionID);

  int getCollectionMinuteToProcess(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId);

  void bumpCollectionMinuteToProcess(
      StateType stateType, String stateExecutionId, String workflowExecutionId, String serviceId, int analysisMinute);
}
