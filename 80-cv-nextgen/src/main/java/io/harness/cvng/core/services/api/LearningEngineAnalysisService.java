package io.harness.cvng.core.services.api;

import io.harness.cvng.statemachine.entities.AnalysisStatus;

import java.util.List;
import java.util.Map;

public interface LearningEngineAnalysisService {
  List<String> createLearningEngineTask();
  Map<String, AnalysisStatus> getTaskStatus(List<String> taskIds);
}
