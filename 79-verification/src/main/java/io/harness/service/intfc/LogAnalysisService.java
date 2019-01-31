package io.harness.service.intfc;

import io.harness.beans.SortOrder.OrderType;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface LogAnalysisService {
  @ValidationGroups(Create.class)
  Boolean saveLogData(@NotNull StateType stateType, String accountId, @NotNull String appId, String cvConfigId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId,
      ClusterLevel clusterLevel, String delegateTaskId, @Valid List<LogElement> logData);

  boolean saveClusteredLogData(String appId, String cvConfigId, ClusterLevel clusterLevel, int logCollectionMinute,
      String host, List<LogElement> logData);

  @ValidationGroups(Create.class)
  Set<LogDataRecord> getLogData(LogRequest logRequest, boolean compareCurrent, String workflowExecutionId,
      ClusterLevel clusterLevel, StateType stateType);

  Set<LogDataRecord> getLogData(String appId, String cvConfigId, ClusterLevel clusterLevel, int logCollectionMinute,
      int startMinute, int endMinute, LogRequest logRequest);

  boolean saveLogAnalysisRecords(LogMLAnalysisRecord mlAnalysisResponse, StateType stateType, Optional<String> taskId);

  boolean save24X7LogAnalysisRecords(String appId, String cvConfigId, int analysisMinute,
      LogMLAnalysisRecord mlAnalysisResponse, Optional<String> taskId);

  LogMLAnalysisRecord getLogAnalysisRecords(
      String appId, String stateExecutionId, String query, StateType stateType, int logCollectionMinute);

  LogMLAnalysisRecord getLogAnalysisRecords(String appId, String cvConfigId, int analysisMinute);

  LogMLAnalysisSummary getExperimentalAnalysisSummary(
      String stateExecutionId, String appId, StateType stateType, String expName);

  List<LogMLExpAnalysisInfo> getExpAnalysisInfoList();

  boolean reQueueExperimentalTask(String appId, String stateExecutionId);

  boolean isBaselineCreated(AnalysisComparisonStrategy comparisonStrategy, StateType stateType, String appId,
      String workflowId, String workflowExecutionId, String serviceId, String query);

  void deleteClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, long logCollectionMinute, ClusterLevel... clusterLevels);

  String getLastSuccessfulWorkflowExecutionIdWithLogs(
      StateType stateType, String appId, String serviceId, String workflowId, String query);

  boolean saveFeedback(LogMLFeedback feedback, StateType stateType);

  boolean saveExperimentalLogAnalysisRecords(
      ExperimentalLogMLAnalysisRecord mlAnalysisResponse, StateType stateType, Optional<String> taskId);

  List<LogMLFeedbackRecord> getMLFeedback(
      String appId, String serviceId, String workflowId, String workflowExecutionId);

  boolean deleteFeedback(String feedbackId);

  Map<String, InstanceElement> getLastExecutionNodes(String appId, String workflowId);

  boolean isStateValid(String appId, String stateExecutionID);

  boolean isProcessingComplete(
      String query, String appId, String stateExecutionId, StateType type, int timeDurationMins);

  long getCollectionMinuteForLevel(String query, String appId, String stateExecutionId, StateType type,
      ClusterLevel clusterLevel, Set<String> testNodes);

  boolean hasDataRecords(String query, String appId, String stateExecutionId, StateType type, Set<String> nodes,
      ClusterLevel level, long logCollectionMinute);

  void bumpClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, long logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel);

  boolean isLogDataCollected(
      String appId, String stateExecutionId, String query, long logCollectionMinute, StateType splunkv2);

  LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String appId, StateType stateType);

  void createAndSaveSummary(StateType stateType, String appId, String stateExecutionId, String query, String message);

  Optional<LogDataRecord> getHearbeatRecordForL0(String appId, String stateExecutionId, StateType type, String host);

  long getMaxCVCollectionMinute(String appId, String cvConfigId);

  long getLogRecordMinute(String appId, String cvConfigId, ClusterLevel clusterLevel, OrderType orderType);

  Set<String> getHostsForMinute(String appId, String cvConfigId, long logRecordMinute, ClusterLevel... clusterLevels);

  long getLastCVAnalysisMinute(String appId, String cvConfigId);

  List<LogDataRecord> getLogRecords(
      String appId, String cvConfigId, ClusterLevel clusterLevel, long startMin, long endMin);
}
