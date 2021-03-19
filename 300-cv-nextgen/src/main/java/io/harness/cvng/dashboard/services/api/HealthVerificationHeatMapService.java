package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.activity.beans.ActivityVerificationResultDTO.CategoryRisk;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface HealthVerificationHeatMapService {
  void updateRisk(String verificationTaskId, Double overallRisk, Instant endTime,
      HealthVerificationPeriod healthVerificationPeriod);
  Optional<Risk> getVerificationRisk(String accountId, String verificationJobInstanceId);
  Set<CategoryRisk> getAggregatedRisk(String activityId, HealthVerificationPeriod healthVerificationPeriod);

  Set<CategoryRisk> getVerificationJobInstanceAggregatedRisk(
      String accountId, String verificationJobInstanceId, HealthVerificationPeriod healthVerificationPeriod);
}
