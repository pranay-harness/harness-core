package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;

import java.util.List;

public interface DeploymentLogAnalysisService {
  void save(DeploymentLogAnalysis deploymentLogAnalysisDTO);

  List<DeploymentLogAnalysis> getAnalysisResults(String verificationTaskId);
}
