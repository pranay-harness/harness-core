package io.harness.cvng.analysis.services.api;

import java.util.Optional;

public interface DeploymentAnalysisService {
  Optional<Double> getLatestRiskScore(String accountId, String verificationJobInstanceId);
}
