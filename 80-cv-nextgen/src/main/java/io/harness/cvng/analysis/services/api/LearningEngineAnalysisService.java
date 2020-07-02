package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LearningEngineAnalysisService {
  LearningEngineTask getNextAnalysisTask();
  LearningEngineTask getNextAnalysisTask(List<LearningEngineTaskType> taskType);
  List<String> createLearningEngineTasks(List<LearningEngineTask> tasks);
  Map<String, ExecutionStatus> getTaskStatus(Set<String> taskIds);
  void markCompleted(String taskId);
}
