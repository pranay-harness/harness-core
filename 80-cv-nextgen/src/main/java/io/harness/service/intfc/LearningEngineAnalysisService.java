package io.harness.service.intfc;

import io.harness.statemachine.entity.AnalysisStatus;

import java.util.List;
import java.util.Map;

public interface LearningEngineAnalysisService {
  List<String> createLearningEngineTask();
  Map<String, AnalysisStatus> getTaskStatus(List<String> taskIds);
}
