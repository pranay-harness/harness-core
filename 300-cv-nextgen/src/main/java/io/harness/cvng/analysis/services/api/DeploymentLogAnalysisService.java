package io.harness.cvng.analysis.services.api;

import io.harness.beans.NGPageResponse;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;

import java.util.List;

public interface DeploymentLogAnalysisService {
  void save(DeploymentLogAnalysis deploymentLogAnalysisDTO);

  List<DeploymentLogAnalysis> getAnalysisResults(String verificationTaskId);

  NGPageResponse<LogAnalysisClusterDTO> getLogAnalysisResult(
      String accountId, String verificationJobInstanceId, Integer label, int pageNumber);
}
