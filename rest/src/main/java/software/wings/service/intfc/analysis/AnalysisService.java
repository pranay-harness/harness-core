package software.wings.service.intfc.analysis;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
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
import java.util.Optional;
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
      ClusterLevel clusterLevel, String delegateTaskId, @Valid List<LogElement> logData) throws IOException;

  @ValidationGroups(Create.class)
  List<LogDataRecord> getLogData(
      @Valid LogRequest logRequest, boolean compareCurrent, ClusterLevel clusterLevel, StateType splunkv2);

  boolean isLogDataCollected(
      String applicationId, String stateExecutionId, String query, int logCollectionMinute, StateType splunkv2);

  Boolean saveLogAnalysisRecords(LogMLAnalysisRecord mlAnalysisResponse, StateType stateType);

  LogMLAnalysisRecord getLogAnalysisRecords(
      String applicationId, String stateExecutionId, String query, StateType stateType, Integer logCollectionMinute);

  LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String applicationId, StateType stateType);

  void validateConfig(@NotNull SettingAttribute settingAttribute, StateType stateType);

  boolean isBaselineCreated(AnalysisComparisonStrategy comparisonStrategy, StateType stateType, String applicationId,
      String workflowId, String workflowExecutionId, String serviceId, String query);

  Object getLogSample(String accountId, String analysisServerConfigId, String index, StateType stateType);

  boolean purgeLogs();

  void createAndSaveSummary(String stateType, String appId, String stateExecutionId, String query, String message);

  void bumpClusterLevel(String stateType, String stateExecutionId, String appId, String searchQuery, Set<String> host,
      int logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel);

  void deleteClusterLevel(String stateType, String stateExecutionId, String appId, String searchQuery, Set<String> host,
      int logCollectionMinute, ClusterLevel... clusterLevels);

  int getLastAnalysisMinute(String stateExecutionId, String applicationId, StateType stateType);

  boolean isStateValid(String appdId, String stateExecutionID);

  int getCollectionMinuteForL1(
      String query, String appdId, String stateExecutionId, String type, Set<String> testNodes);

  Optional<LogDataRecord> getLogDataRecordForL0(String appId, String stateExecutionId, String type);

  boolean isProcessingComplete(String query, String appId, String stateExecutionId, String type, int timeDurationMins);

  boolean hasDataRecords(String query, String appdId, String stateExecutionId, String type, Set<String> nodes,
      ClusterLevel level, int logCollectionMinute);
}
