package io.harness.cvng.analysis.services.impl;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.services.api.LearningEngineAnalysisService;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LearningEngineAnalysisServiceImpl implements LearningEngineAnalysisService {
  @Override
  public LearningEngineTask getNextAnalysisTask() {
    return null;
  }

  @Override
  public LearningEngineTask getNextAnalysisTask(List<LearningEngineTaskType> taskType) {
    return null;
  }

  @Override
  public List<String> createLearningEngineTasks(List<LearningEngineTask> tasks) {
    return null;
  }

  @Override
  public Map<String, ExecutionStatus> getTaskStatus(Set<String> taskIds) {
    return null;
  }

  @Override
  public void markCompleted(String taskId) {}
}
