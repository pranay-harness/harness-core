package software.wings.service.intfc.analysis;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.AnalysisComparisonStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.sm.StateType;
import software.wings.utils.validation.Create;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AnalysisService {
  @ValidationGroups(Create.class)
  Boolean saveLogData(@NotNull StateType stateType, String accountId, @NotNull String appId,
      @NotNull String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId,
      ClusterLevel clusterLevel, @Valid List<LogElement> logData) throws IOException;

  @ValidationGroups(Create.class)
  List<LogDataRecord> getLogData(
      @Valid LogRequest logRequest, boolean compareCurrent, ClusterLevel clusterLevel, StateType splunkv2);

  void finalizeLogCollection(String accountId, StateType stateType, String workflowExecutionId, LogRequest logRequest);

  boolean deleteProcessed(LogRequest logRequest, StateType stateType, ClusterLevel clusterLevel);

  boolean isLogDataCollected(
      String applicationId, String stateExecutionId, String query, int logCollectionMinute, StateType splunkv2);

  Boolean saveLogAnalysisRecords(LogMLAnalysisRecord mlAnalysisResponse, StateType stateType);

  LogMLAnalysisRecord getLogAnalysisRecords(
      String applicationId, String stateExecutionId, String query, StateType stateType);

  LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String applicationId, StateType stateType);

  void validateConfig(@NotNull SettingAttribute settingAttribute, StateType stateType);

  boolean isBaselineCreated(AnalysisComparisonStrategy comparisonStrategy, StateType stateType, String applicationId,
      String workflowId, String workflowExecutionId, String serviceId, String query);
}
