package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.CanaryBlueGreenAdditionalInfo;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.core.beans.LoadTestAdditionalInfo;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import java.util.Optional;

public interface DeploymentAnalysisService {
  Optional<Risk> getLatestRiskScore(String accountId, String verificationJobInstanceId);

  CanaryBlueGreenAdditionalInfo getCanaryBlueGreenAdditionalInfo(
      String accountId, VerificationJobInstance verificationJobInstance);

  LoadTestAdditionalInfo getLoadTestAdditionalInfo(String accountId, VerificationJobInstance verificationJobInstance);
}
