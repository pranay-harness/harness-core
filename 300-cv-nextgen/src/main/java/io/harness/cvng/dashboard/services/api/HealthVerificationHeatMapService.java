package io.harness.cvng.dashboard.services.api;

import io.harness.cvng.activity.beans.ActivityVerificationResultDTO.CategoryRisk;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface HealthVerificationHeatMapService {
  void updateRisk(String verificationTaskId, Double overallRisk, Instant endTime,
      HealthVerificationPeriod healthVerificationPeriod);
  Optional<Double> getVerificationRisk(VerificationJobInstance verificationJobInstance);
  List<Optional<Double>> getRisksOfVerification(List<VerificationJobInstance> verificationJobInstances);
  Set<CategoryRisk> getAggregatedRisk(String activityId, HealthVerificationPeriod healthVerificationPeriod);
}
