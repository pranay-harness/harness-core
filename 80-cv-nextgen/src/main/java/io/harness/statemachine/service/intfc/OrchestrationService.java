package io.harness.statemachine.service.intfc;

import java.time.Instant;

public interface OrchestrationService {
  void queueAnalysis(String cvConfigId, Instant startTime, Instant endTime);
  void orchestrate(String cvConfigId);
}
