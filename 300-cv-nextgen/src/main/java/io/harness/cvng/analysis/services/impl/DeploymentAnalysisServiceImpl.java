package io.harness.cvng.analysis.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.analysis.services.api.DeploymentAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;

import java.util.Optional;

public class DeploymentAnalysisServiceImpl implements DeploymentAnalysisService {
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Override
  public Optional<Double> getLatestRiskScore(String accountId, String verificationJobInstanceId) {
    Optional<Double> latestTimeSeriesRiskScore =
        deploymentTimeSeriesAnalysisService.getLatestRiskScore(accountId, verificationJobInstanceId);
    Optional<Double> latestLogRiskScore =
        deploymentLogAnalysisService.getLatestRiskScore(accountId, verificationJobInstanceId);
    if (latestTimeSeriesRiskScore.isPresent() && latestLogRiskScore.isPresent()) {
      return Optional.of(Math.max(latestTimeSeriesRiskScore.get(), latestLogRiskScore.get()));
    } else if (latestTimeSeriesRiskScore.isPresent()) {
      return latestTimeSeriesRiskScore;
    } else if (latestLogRiskScore.isPresent()) {
      return latestLogRiskScore;
    } else {
      return Optional.empty();
    }
  }
}
