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
