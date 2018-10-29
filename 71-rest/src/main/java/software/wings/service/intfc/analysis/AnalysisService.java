package software.wings.service.intfc.analysis;

import software.wings.api.InstanceElement;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AnalysisService {
  Boolean saveLogAnalysisRecords(LogMLAnalysisRecord mlAnalysisResponse, StateType stateType, Optional<String> taskId);

  LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String appId, StateType stateType);

  void validateConfig(
      @NotNull SettingAttribute settingAttribute, StateType stateType, List<EncryptedDataDetail> encryptedDataDetails);

  boolean isBaselineCreated(AnalysisComparisonStrategy comparisonStrategy, StateType stateType, String appId,
      String workflowId, String workflowExecutionId, String serviceId);

  /**
   * Method to return Sample data for given configurations.
   * @param accountId
   * @param analysisServerConfigId
   * @param index
   * @param stateType
   * @param duration
   * @return
   */
  Object getLogSample(String accountId, String analysisServerConfigId, String index, StateType stateType, int duration);

  void createAndSaveSummary(StateType stateType, String appId, String stateExecutionId, String query, String message);

  void bumpClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, int logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel);

  void deleteClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, int logCollectionMinute, ClusterLevel... clusterLevels);

  boolean isStateValid(String appId, String stateExecutionID);

  String getLastSuccessfulWorkflowExecutionIdWithLogs(
      StateType stateType, String appId, String serviceId, String workflowId);

  boolean saveFeedback(LogMLFeedback feedback, StateType stateType);

  List<LogMLFeedbackRecord> getMLFeedback(
      String appId, String serviceId, String workflowId, String workflowExecutionId);

  void cleanUpForLogRetry(String stateExecutionId);

  boolean deleteFeedback(String feedbackId) throws IOException;

  LogMLAnalysisSummary getAnalysisSummaryForDemo(String stateExecutionId, String applicationId, StateType stateType);

  Object getHostLogRecords(String accountId, String analysisServerConfigId, String index, ElkQueryType queryType,
      String query, String timeStampField, String timeStampFieldFormat, String messageField, String hostNameField,
      String hostName, StateType stateType, boolean formattedQuery);

  Map<String, InstanceElement> getLastExecutionNodes(String appId, String workflowId);
}
