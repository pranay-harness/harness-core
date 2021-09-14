/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.statemachine.beans.AnalysisStatus;

import java.time.Instant;

public interface HealthVerificationService {
  Instant aggregateActivityAnalysis(String verificationTaskId, Instant startTime, Instant endTime,
      Instant latestTimeOfAnalysis, HealthVerificationPeriod healthVerificationPeriod);
  void updateProgress(
      String verificationTaskId, Instant latestTimeOfAnalysis, AnalysisStatus status, boolean isFinalState);
}
