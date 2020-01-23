package io.harness.service.intfc;

import io.harness.beans.ExecutionStatus;
import io.harness.service.LearningEngineError;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.intfc.analysis.ClusterLevel;

import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 1/9/18.
 */
public interface LearningEngineService {
  String RESOURCE_URL = "learning";

  LearningEngineAnalysisTask getTaskById(String taskId);

  boolean addLearningEngineAnalysisTask(LearningEngineAnalysisTask analysisTask);
  boolean addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask analysisTask);

  LearningEngineAnalysisTask getNextLearningEngineAnalysisTask(
      ServiceApiVersion serviceApiVersion, Optional<String> is24x7Task, Optional<List<MLAnalysisType>> taskTypes);
  LearningEngineExperimentalAnalysisTask getNextLearningEngineExperimentalAnalysisTask(
      ServiceApiVersion serviceApiVersion, String experimentName, Optional<List<MLAnalysisType>> taskTypes);

  boolean hasAnalysisTimedOut(String appId, String workflowExecutionId, String stateExecutionId);
  List<MLExperiments> getExperiments(MLAnalysisType ml_analysis_type);

  void markCompleted(String taskId);
  void markExpTaskCompleted(String taskId);

  void markStatus(
      String workflowExecutionId, String stateExecutionId, long analysisMinute, ExecutionStatus executionStatus);
  void markCompleted(String workflowExecutionId, String stateExecutionId, long analysisMinute, MLAnalysisType type,
      ClusterLevel level);
  void initializeServiceSecretKeys();

  String getServiceSecretKey(ServiceType serviceType);

  AnalysisContext getNextVerificationAnalysisTask(ServiceApiVersion serviceApiVersion);

  void markJobStatus(AnalysisContext verificationAnalysisTask, ExecutionStatus executionStatus);
  void checkAndUpdateFailedLETask(String stateExecutionId, int analysisMinute);

  boolean isStateValid(String appId, String stateExecutionId);

  boolean notifyFailure(String taskId, LearningEngineError learningEngineError);
  int getNextServiceGuardBackoffCount(
      String stateExecutionId, String cvConfig, long analysisMinute, MLAnalysisType analysisType);
  boolean isEligibleToCreateTask(
      String stateExecutionId, String cvConfig, long analysisMinute, MLAnalysisType analysisType);
  boolean shouldUseSupervisedModel(String fieldName, String fieldValue);
  String getServiceIdFromStateExecutionId(String stateExecutionId);
  boolean isTaskRunningOrQueued(String cvConfigId, long analysisMinute);
}
