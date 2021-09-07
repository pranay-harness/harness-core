package io.harness.cvng.analysis.services.api;

import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.TimeSeriesAnalysisSummary;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentTimeSeriesAnalysisFilter;

import java.util.List;
import java.util.Optional;

public interface DeploymentTimeSeriesAnalysisService {
  void save(DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis);
  TransactionMetricInfoSummaryPageDTO getMetrics(String accountId, String verificationJobInstanceId,
      DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter, PageParams pageParams);
  List<DeploymentTimeSeriesAnalysis> getAnalysisResults(String verificationTaskId);
  Optional<Risk> getRecentHighestRiskScore(String accountId, String verificationJobInstanceId);

  DeploymentTimeSeriesAnalysis getRecentHighestDeploymentTimeSeriesAnalysis(
      String accountId, String verificationJobInstanceId);

  List<DeploymentTimeSeriesAnalysis> getLatestDeploymentTimeSeriesAnalysis(String accountId,
      String verificationJobInstanceId, DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter);

  TimeSeriesAnalysisSummary getAnalysisSummary(List<String> verificationJobInstanceIds);
}
