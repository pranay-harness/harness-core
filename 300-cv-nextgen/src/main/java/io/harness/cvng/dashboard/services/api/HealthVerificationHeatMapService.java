package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.analysis.entities.HealthVerificationPeriod;

import java.time.Instant;

public interface HealthVerificationHeatMapService {
  void updateRisk(String verificationTaskId, Double overallRisk, Instant endTime,
      HealthVerificationPeriod healthVerificationPeriod);
}
