package software.wings.service.intfc;

import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.ExecutionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 1/9/18.
 */
public interface LearningEngineService {
  String RESOURCE_URL = "learning";
  boolean addLearningEngineAnalysisTask(LearningEngineAnalysisTask analysisTask);
  boolean addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask analysisTask);

  LearningEngineAnalysisTask getNextLearningEngineAnalysisTask(ServiceApiVersion serviceApiVersion);
  LearningEngineExperimentalAnalysisTask getNextLearningEngineExperimentalAnalysisTask(
      String experimentName, ServiceApiVersion serviceApiVersion);

  boolean hasAnalysisTimedOut(String appId, String workflowExecutionId, String stateExecutionId);
  List<MLExperiments> getExperiments(MLAnalysisType ml_analysis_type);

  void markCompleted(String taskId);
  void markExpTaskCompleted(String taskId);

  void markStatus(
      String workflowExecutionId, String stateExecutionId, int analysisMinute, ExecutionStatus executionStatus);
  void markCompleted(String workflowExecutionId, String stateExecutionId, int analysisMinute, MLAnalysisType type);
  void markCompleted(
      String workflowExecutionId, String stateExecutionId, int analysisMinute, MLAnalysisType type, ClusterLevel level);
  void initializeServiceSecretKeys();

  String getServiceSecretKey(ServiceType serviceType);

  void cleanup(long keepAfterTimeMillis);

  Optional<LearningEngineAnalysisTask> earliestQueued();
}
