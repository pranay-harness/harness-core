package io.harness.cvng.analysis.services.api;

import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;

import java.util.List;
import java.util.Optional;

public interface DeploymentTimeSeriesAnalysisService {
  void save(DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis);
  TransactionMetricInfoSummaryPageDTO getMetrics(String accountId, String verificationJobInstanceId,
      boolean anomalousMetricsOnly, String hostName, int pageNumber);
  List<DeploymentTimeSeriesAnalysis> getAnalysisResults(String verificationTaskId);
  Optional<Double> getLatestRiskScore(String accountId, String verificationJobInstanceId);
}
