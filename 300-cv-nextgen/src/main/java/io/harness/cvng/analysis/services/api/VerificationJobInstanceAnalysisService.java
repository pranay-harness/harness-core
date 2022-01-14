package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.CanaryBlueGreenAdditionalInfo;
import io.harness.cvng.analysis.beans.HealthAdditionalInfo;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.core.beans.LoadTestAdditionalInfo;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import java.util.Optional;

public interface VerificationJobInstanceAnalysisService {
  Optional<Risk> getLatestRiskScore(String accountId, String verificationJobInstanceId);

  CanaryBlueGreenAdditionalInfo getCanaryBlueGreenAdditionalInfo(
      String accountId, VerificationJobInstance verificationJobInstance);

  LoadTestAdditionalInfo getLoadTestAdditionalInfo(String accountId, VerificationJobInstance verificationJobInstance);

  HealthAdditionalInfo getHealthAdditionInfo(String accountId, VerificationJobInstance verificationJobInstance);

  void addDemoAnalysisData(
      String verificationTaskId, CVConfig cvConfig, VerificationJobInstance verificationJobInstance);
}
