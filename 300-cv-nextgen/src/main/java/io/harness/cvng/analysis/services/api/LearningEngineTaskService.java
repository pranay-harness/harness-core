/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LearningEngineTaskService {
  LearningEngineTask getNextAnalysisTask();
  LearningEngineTask getNextAnalysisTask(List<LearningEngineTaskType> taskType);
  List<String> createLearningEngineTasks(List<LearningEngineTask> tasks);
  String createLearningEngineTask(LearningEngineTask learningEngineTask);
  Map<String, ExecutionStatus> getTaskStatus(Set<String> taskIds);
  void markCompleted(String taskId);
  void markFailure(String taskId);
  String createFailureUrl(String taskId);
  LearningEngineTask get(String learningEngineTaskId);
}
