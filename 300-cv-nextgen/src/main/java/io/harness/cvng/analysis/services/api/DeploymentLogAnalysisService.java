package io.harness.cvng.analysis.services.api;

import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.LogsAnalysisSummary;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentLogAnalysisFilter;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import java.util.Optional;

public interface DeploymentLogAnalysisService {
  void save(DeploymentLogAnalysis deploymentLogAnalysisDTO);

  List<DeploymentLogAnalysis> getAnalysisResults(String verificationTaskId);

  List<LogAnalysisClusterChartDTO> getLogAnalysisClusters(
      String accountId, String verificationJobInstanceId, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter);

  Optional<Risk> getRecentHighestRiskScore(String accountId, String verificationJobInstanceId);

  DeploymentLogAnalysis getRecentHighestDeploymentLogAnalysis(String accountId, String verificationJobInstanceId);

  LogsAnalysisSummary getAnalysisSummary(String accountId, List<String> verificationJobInstanceIds);

  PageResponse<LogAnalysisClusterDTO> getLogAnalysisResult(String accountId, String verificationJobInstanceId,
      Integer label, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter, PageParams pageParams);

  List<DeploymentLogAnalysis> getLatestDeploymentLogAnalysis(
      String accountId, String verificationJobInstanceId, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter);
}
