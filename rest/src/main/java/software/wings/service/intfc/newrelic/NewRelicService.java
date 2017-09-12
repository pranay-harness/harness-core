package software.wings.service.intfc.newrelic;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateExecutionInstance;
import software.wings.utils.validation.Create;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicService {
  void validateConfig(@NotNull SettingAttribute settingAttribute);

  List<NewRelicApplication> getApplications(@NotNull String settingId);

  @ValidationGroups(Create.class)
  boolean saveMetricData(@NotNull String accountId, String applicationId,
      @Valid List<NewRelicMetricDataRecord> metricData) throws IOException;

  @ValidationGroups(Create.class) boolean saveAnalysisRecords(@Valid NewRelicMetricAnalysisRecord metricAnalysisRecord);

  List<NewRelicMetricDataRecord> getRecords(String workflowExecutionId, String stateExecutionId, String workflowId,
      String serviceId, Set<String> nodes, int analysisMinute);

  List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(String workflowId, String serviceId, int analysisMinute);

  NewRelicMetricAnalysisRecord getMetricsAnalysis(String stateExecutionId, String workflowExecutionId);

  boolean isStateValid(String appdId, String stateExecutionID);

  int getCollectionMinuteToProcess(String stateExecutionId, String workflowExecutionId, String serviceId);

  void bumpCollectionMinuteToProcess(
      String stateExecutionId, String workflowExecutionId, String serviceId, int analysisMinute);
}
