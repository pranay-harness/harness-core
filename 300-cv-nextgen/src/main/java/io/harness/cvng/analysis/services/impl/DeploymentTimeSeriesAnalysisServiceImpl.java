package io.harness.cvng.analysis.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TransactionSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis.DeploymentTimeSeriesAnalysisKeys;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.persistence.HPersistence;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DeploymentTimeSeriesAnalysisServiceImpl implements DeploymentTimeSeriesAnalysisService {
  public static final int DEFAULT_PAGE_SIZE = 10;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationTaskService verificationTaskService;

  @Override
  public void save(DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis) {
    hPersistence.save(deploymentTimeSeriesAnalysis);
  }

  @Override
  public TransactionSummaryPageDTO getMetrics(String accountId, String verificationJobInstanceId,
      boolean anomalousMetricsOnly, String hostName, int pageNumber) {
    Set<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId);

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis =
        getLatestDeploymentTimeSeriesAnalysis(verificationTaskIds);
    Optional<DeploymentTimeSeriesAnalysisDTO.ResultSummary> resultSummary =
        getFilteredResultSummary(deploymentTimeSeriesAnalysis, hostName, anomalousMetricsOnly);

    List<DeploymentTimeSeriesAnalysisDTO.TransactionSummary> transactionSummaries =
        resultSummary.isPresent() ? resultSummary.get().getTransactionSummaries() : Collections.emptyList();

    int numberOfPages = TransactionSummaryPageDTO.setNumberOfPages(transactionSummaries.size(), DEFAULT_PAGE_SIZE);

    if (pageNumber == 0 || pageNumber > numberOfPages) {
      throw new IllegalArgumentException("Invalid page number " + pageNumber);
    }
    return TransactionSummaryPageDTO.builder()
        .transactionSummaries(TransactionSummaryPageDTO.getPage(transactionSummaries, pageNumber, DEFAULT_PAGE_SIZE))
        .numberOfPages(numberOfPages)
        .elementRange(TransactionSummaryPageDTO.setElementRange(transactionSummaries, pageNumber, DEFAULT_PAGE_SIZE))
        .pageNumber(pageNumber)
        .build();
  }

  @Override
  public List<DeploymentTimeSeriesAnalysis> getAnalysisResults(String verificationTaskId) {
    return hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class)
        .filter(DeploymentTimeSeriesAnalysisKeys.verificationTaskId, verificationTaskId)
        .asList();
  }

  private DeploymentTimeSeriesAnalysis getLatestDeploymentTimeSeriesAnalysis(Set<String> verificationTaskIds) {
    return hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class)
        .field(DeploymentTimeSeriesAnalysisKeys.verificationTaskId)
        .in(verificationTaskIds)
        .order(Sort.descending(DeploymentTimeSeriesAnalysisKeys.startTime))
        .get();
  }

  private Optional<DeploymentTimeSeriesAnalysisDTO.ResultSummary> fetchResultSummaryFromHostName(
      DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis, String hostName) {
    return deploymentTimeSeriesAnalysis.getHostSummaries()
        .stream()
        .filter(hostSummary -> hostName.equals(hostSummary.getHostName()))
        .map(DeploymentTimeSeriesAnalysisDTO.HostSummary::getResultSummary)
        .findFirst();
  }

  private Optional<DeploymentTimeSeriesAnalysisDTO.ResultSummary> getFilteredResultSummary(
      DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis, String hostName, boolean anomalousMetricsOnly) {
    if (deploymentTimeSeriesAnalysis != null) {
      if (StringUtils.isNotBlank(hostName) && anomalousMetricsOnly) {
        Optional<DeploymentTimeSeriesAnalysisDTO.ResultSummary> resultSummary =
            fetchResultSummaryFromHostName(deploymentTimeSeriesAnalysis, hostName);
        if (resultSummary.isPresent() && resultSummary.get().getRisk() < 1) {
          resultSummary = Optional.empty();
        }
        return resultSummary.isPresent() ? resultSummary : Optional.empty();
      }

      if (StringUtils.isNotBlank(hostName) && !anomalousMetricsOnly) {
        return fetchResultSummaryFromHostName(deploymentTimeSeriesAnalysis, hostName);
      }

      if (StringUtils.isBlank(hostName) && anomalousMetricsOnly) {
        return deploymentTimeSeriesAnalysis.getResultSummary().getRisk() >= 1
            ? Optional.of(deploymentTimeSeriesAnalysis.getResultSummary())
            : Optional.empty();
      }

      return Optional.of(deploymentTimeSeriesAnalysis.getResultSummary());
    }
    return Optional.empty();
  }
}
