package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.entities.DeploymentVerificationTaskTimeSeriesAnalysis;

import java.util.List;

public interface DeploymentVerificationTaskTimeSeriesAnalysisService {
  void save(DeploymentVerificationTaskTimeSeriesAnalysis deploymentVerificationTaskTimeSeriesAnalysis);

  List<DeploymentVerificationTaskTimeSeriesAnalysis> getAnalysisResults(String verificationTaskId);
}
