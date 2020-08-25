package io.harness.cvng.analysis.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.analysis.entities.DeploymentVerificationTaskTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.DeploymentVerificationTaskTimeSeriesAnalysis.DeploymentVerificationTaskTimeSeriesAnalysisKeys;
import io.harness.cvng.analysis.services.api.DeploymentVerificationTaskTimeSeriesAnalysisService;
import io.harness.persistence.HPersistence;

import java.util.List;

public class DeploymentVerificationTaskTimeSeriesAnalysisServiceImpl
    implements DeploymentVerificationTaskTimeSeriesAnalysisService {
  @Inject private HPersistence hPersistence;
  @Override
  public void save(DeploymentVerificationTaskTimeSeriesAnalysis deploymentVerificationTaskTimeSeriesAnalysis) {
    hPersistence.save(deploymentVerificationTaskTimeSeriesAnalysis);
  }

  @Override
  public List<DeploymentVerificationTaskTimeSeriesAnalysis> getAnalysisResults(String verificationTaskId) {
    return hPersistence.createQuery(DeploymentVerificationTaskTimeSeriesAnalysis.class)
        .filter(DeploymentVerificationTaskTimeSeriesAnalysisKeys.verificationTaskId, verificationTaskId)
        .asList();
  }
}
