package io.harness.cvng.statemachine.services.intfc;

import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;

import java.time.Instant;
import java.util.Set;

public interface OrchestrationService {
  void queueAnalysis(String verificationTaskId, Instant startTime, Instant endTime);
  void orchestrate(String verificationTaskId);
  void orchestrate(AnalysisOrchestrator orchestrator);
  void recordMetrics();
  void markCompleted(String verificationTaskId);
  void markCompleted(Set<String> verificationTaskIds);
}
