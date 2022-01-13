/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.intfc;

import io.harness.beans.ExecutionStatus;
import io.harness.service.LearningEngineError;
import io.harness.version.ServiceApiVersion;

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
  LearningEngineAnalysisTask getNextLearningEngineAnalysisTask(ServiceApiVersion serviceApiVersion);
  LearningEngineExperimentalAnalysisTask getNextLearningEngineExperimentalAnalysisTask(
      ServiceApiVersion serviceApiVersion, String experimentName, Optional<List<MLAnalysisType>> taskTypes);

  boolean hasAnalysisTimedOut(String appId, String workflowExecutionId, String stateExecutionId);
  List<MLExperiments> getExperiments(MLAnalysisType ml_analysis_type);

  void markCompleted(String taskId);
  void markExpTaskCompleted(String taskId);

  void markStatus(
      String workflowExecutionId, String stateExecutionId, long analysisMinute, ExecutionStatus executionStatus);
  void markCompleted(String accountId, String workflowExecutionId, String stateExecutionId, long analysisMinute,
      MLAnalysisType type, ClusterLevel level);

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
  boolean isTaskRunningOrQueued(String cvConfigId);
}
