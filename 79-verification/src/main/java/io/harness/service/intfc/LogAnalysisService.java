package io.harness.service.intfc;

import io.harness.validation.Create;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLExpAnalysisInfo;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface LogAnalysisService {
  @ValidationGroups(Create.class)
  Boolean saveLogData(@NotNull StateType stateType, String accountId, @NotNull String appId,
      @NotNull String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId,
      ClusterLevel clusterLevel, String delegateTaskId, @Valid List<LogElement> logData) throws IOException;

  @ValidationGroups(Create.class)
  List<LogDataRecord> getLogData(LogRequest logRequest, boolean compareCurrent, String workflowExecutionId,
      ClusterLevel clusterLevel, StateType stateType);

  Boolean saveLogAnalysisRecords(LogMLAnalysisRecord mlAnalysisResponse, StateType stateType, Optional<String> taskId);

  LogMLAnalysisRecord getLogAnalysisRecords(String appId, String stateExecutionId, String query, StateType stateType,
      Integer logCollectionMinute) throws IOException;

  LogMLAnalysisSummary getExperimentalAnalysisSummary(
      String stateExecutionId, String appId, StateType stateType, String expName);

  List<LogMLExpAnalysisInfo> getExpAnalysisInfoList();

  boolean reQueueExperimentalTask(String appId, String stateExecutionId);

  boolean isBaselineCreated(AnalysisComparisonStrategy comparisonStrategy, StateType stateType, String appId,
      String workflowId, String workflowExecutionId, String serviceId);

  boolean purgeLogs();

  void deleteClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, int logCollectionMinute, ClusterLevel... clusterLevels);

  String getLastSuccessfulWorkflowExecutionIdWithLogs(
      StateType stateType, String appId, String serviceId, String workflowId);

  boolean saveFeedback(LogMLFeedback feedback, StateType stateType);

  boolean saveExperimentalLogAnalysisRecords(
      ExperimentalLogMLAnalysisRecord mlAnalysisResponse, StateType stateType, Optional<String> taskId);

  List<LogMLFeedbackRecord> getMLFeedback(
      String appId, String serviceId, String workflowId, String workflowExecutionId);

  void cleanUpForLogRetry(String stateExecutionId);

  boolean deleteFeedback(String feedbackId) throws IOException;

  Map<String, InstanceElement> getLastExecutionNodes(String appId, String workflowId);

  boolean isStateValid(String appId, String stateExecutionID);

  boolean isProcessingComplete(
      String query, String appId, String stateExecutionId, StateType type, int timeDurationMins);

  int getCollectionMinuteForLevel(String query, String appId, String stateExecutionId, StateType type,
      ClusterLevel clusterLevel, Set<String> testNodes);

  boolean hasDataRecords(String query, String appId, String stateExecutionId, StateType type, Set<String> nodes,
      ClusterLevel level, int logCollectionMinute);

  void bumpClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, int logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel);

  boolean isLogDataCollected(
      String appId, String stateExecutionId, String query, int logCollectionMinute, StateType splunkv2);

  LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String appId, StateType stateType);

  void createAndSaveSummary(StateType stateType, String appId, String stateExecutionId, String query, String message);

  Optional<LogDataRecord> getHearbeatRecordForL0(String appId, String stateExecutionId, StateType type, String host);
}
