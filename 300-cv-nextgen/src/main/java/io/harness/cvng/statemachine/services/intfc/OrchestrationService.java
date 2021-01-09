package io.harness.cvng.statemachine.services.intfc;

import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;

import java.time.Instant;

public interface OrchestrationService {
  void queueAnalysis(String cvConfigId, Instant startTime, Instant endTime);
  void orchestrate(String verificationTaskId);
  void orchestrate(AnalysisOrchestrator orchestrator);
}
