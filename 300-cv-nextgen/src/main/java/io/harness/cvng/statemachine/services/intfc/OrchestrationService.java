/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.statemachine.services.intfc;

import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;

import java.time.Instant;
import java.util.Set;

public interface OrchestrationService {
  void queueAnalysis(String verificationTaskId, Instant startTime, Instant endTime);
  void orchestrate(AnalysisOrchestrator orchestrator);
  void recordMetrics();
  void markCompleted(String verificationTaskId);
  void markCompleted(Set<String> verificationTaskIds);
}
