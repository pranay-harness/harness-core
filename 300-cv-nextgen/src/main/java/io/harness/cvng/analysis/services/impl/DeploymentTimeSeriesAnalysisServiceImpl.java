package io.harness.cvng.analysis.services.impl;

import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfo;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis.DeploymentTimeSeriesAnalysisKeys;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.utils.CVNGObjectUtils;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Sort;

public class DeploymentTimeSeriesAnalysisServiceImpl implements DeploymentTimeSeriesAnalysisService {
  public static final int DEFAULT_PAGE_SIZE = 10;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private NextGenService nextGenService;

  @Override
  public void save(DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis) {
    hPersistence.save(deploymentTimeSeriesAnalysis);
  }

  @Override
  public TransactionMetricInfoSummaryPageDTO getMetrics(String accountId, String verificationJobInstanceId,
      boolean anomalousMetricsOnly, String hostName, int pageNumber) {
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);

    DeploymentTimeSeriesAnalysis latestDeploymentTimeSeriesAnalysis =
        getLatestDeploymentTimeSeriesAnalysis(accountId, verificationJobInstanceId);

    if (latestDeploymentTimeSeriesAnalysis == null) {
      return TransactionMetricInfoSummaryPageDTO.builder()
          .pageResponse(formPageResponse(Collections.emptyList(), pageNumber, DEFAULT_PAGE_SIZE))
          .build();
    }

    TimeRange deploymentTimeRange = TimeRange.builder()
                                        .startTime(verificationJobInstance.getStartTime())
                                        .endTime(latestDeploymentTimeSeriesAnalysis.getEndTime())
                                        .build();

    validateHostName(latestDeploymentTimeSeriesAnalysis.getHostSummaries(), hostName);
    Set<TransactionMetricInfo> transactionMetricInfoSet = new HashSet();
    String connectorName = getConnectorName(latestDeploymentTimeSeriesAnalysis);

    latestDeploymentTimeSeriesAnalysis.getTransactionMetricSummaries()
        .stream()
        .filter(transactionMetricHostData -> filterAnomalousMetrics(transactionMetricHostData, anomalousMetricsOnly))
        .forEach(transactionMetricHostData -> {
          TransactionMetricInfo transactionMetricInfo =
              TransactionMetricInfo.builder()
                  .transactionMetric(createTransactionMetric(transactionMetricHostData))
                  .connectorName(connectorName)
                  .build();
          SortedSet<DeploymentTimeSeriesAnalysisDTO.HostData> nodeDataSet = new TreeSet();
          transactionMetricHostData.getHostData()
              .stream()
              .filter(hostData -> filterByHostName(hostData, hostName))
              .forEach(hostData -> nodeDataSet.add(hostData));
          transactionMetricInfo.setNodes(nodeDataSet);
          transactionMetricInfoSet.add(transactionMetricInfo);
        });

    List<TransactionMetricInfo> transactionMetricInfoList =
        transactionMetricInfoSet.stream().collect(Collectors.toList());

    Collections.sort(transactionMetricInfoList,
        (d1, d2) -> Double.compare(d2.getTransactionMetric().getScore(), d1.getTransactionMetric().getScore()));

    return TransactionMetricInfoSummaryPageDTO.builder()
        .pageResponse(formPageResponse(transactionMetricInfoList, pageNumber, DEFAULT_PAGE_SIZE))
        .deploymentTimeRange(deploymentTimeRange)
        .build();
  }

  private PageResponse<TransactionMetricInfo> formPageResponse(
      List<TransactionMetricInfo> transactionMetricInfoList, int pageNumber, int size) {
    List<TransactionMetricInfo> returnList = new ArrayList<>();

    int startIndex = pageNumber * size;
    Iterator<TransactionMetricInfo> iterator = transactionMetricInfoList.iterator();
    int i = 0;
    while (iterator.hasNext()) {
      TransactionMetricInfo transactionMetricInfo = iterator.next();
      if (i >= startIndex && returnList.size() < size) {
        returnList.add(transactionMetricInfo);
      }
      i++;
    }
    return PageResponse.<TransactionMetricInfo>builder()
        .pageSize(size)
        .pageIndex(pageNumber)
        .totalPages(transactionMetricInfoList.size() / size)
        .totalItems(transactionMetricInfoList.size())
        .content(returnList)
        .build();
  }

  private void validateHostName(List<DeploymentTimeSeriesAnalysisDTO.HostInfo> hostSummaries, String hostName) {
    if (StringUtils.isNotBlank(hostName)
        && !hostSummaries.stream()
                .filter(hostSummary -> hostName.equals(hostSummary.getHostName()))
                .findFirst()
                .isPresent()) {
      throw new BadRequestException("Host Name ".concat(hostName).concat(" doesn't exist"));
    }
  }

  private boolean filterByHostName(DeploymentTimeSeriesAnalysisDTO.HostData hostData, String hostName) {
    if (StringUtils.isBlank(hostName)) {
      return true;
    } else if (hostData.getHostName().isPresent()) {
      return hostName.equals(hostData.getHostName().get());
    } else {
      return false;
    }
  }

  private boolean filterAnomalousMetrics(
      DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData,
      boolean anomalousMetricsOnly) {
    return anomalousMetricsOnly ? transactionMetricHostData.getScore() >= 1 : Boolean.TRUE;
  }

  private TransactionMetricInfo.TransactionMetric createTransactionMetric(
      DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData) {
    return TransactionMetricInfo.TransactionMetric.builder()
        .transactionName(transactionMetricHostData.getTransactionName())
        .metricName(transactionMetricHostData.getMetricName())
        .score(transactionMetricHostData.getScore())
        .build();
  }

  @Override
  public List<DeploymentTimeSeriesAnalysis> getAnalysisResults(String verificationTaskId) {
    return hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class)
        .filter(DeploymentTimeSeriesAnalysisKeys.verificationTaskId, verificationTaskId)
        .asList();
  }

  @Override
  public Optional<Double> getRecentHighestRiskScore(String accountId, String verificationJobInstanceId) {
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis =
        getRecentHighestDeploymentTimeSeriesAnalysis(accountId, verificationJobInstanceId);
    if (deploymentTimeSeriesAnalysis == null) {
      return Optional.empty();
    } else {
      return Optional.of(deploymentTimeSeriesAnalysis.getScore());
    }
  }
  @Override
  @Nullable
  public DeploymentTimeSeriesAnalysis getRecentHighestDeploymentTimeSeriesAnalysis(
      String accountId, String verificationJobInstanceId) {
    Set<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId);
    DeploymentTimeSeriesAnalysis max = null;
    for (String verificationTaskId : verificationTaskIds) {
      DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis =
          hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class)
              .filter(DeploymentTimeSeriesAnalysisKeys.verificationTaskId, verificationTaskId)
              .order(Sort.descending(DeploymentTimeSeriesAnalysisKeys.startTime))
              .get();
      max = CVNGObjectUtils.max(
          max, deploymentTimeSeriesAnalysis, Comparator.comparingDouble(DeploymentTimeSeriesAnalysis::getScore));
    }
    return max;
  }

  @Override
  public DeploymentTimeSeriesAnalysis getLatestDeploymentTimeSeriesAnalysis(
      String accountId, String verificationJobInstanceId) {
    Set<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId);
    return hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class)
        .field(DeploymentTimeSeriesAnalysisKeys.verificationTaskId)
        .in(verificationTaskIds)
        .order(Sort.descending(DeploymentTimeSeriesAnalysisKeys.startTime))
        .get();
  }

  @Nullable
  private String getConnectorName(DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis) {
    String cvConfigId = verificationTaskService.getCVConfigId(deploymentTimeSeriesAnalysis.getVerificationTaskId());
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    Optional<ConnectorInfoDTO> connectorInfoDTO = nextGenService.get(cvConfig.getAccountId(),
        cvConfig.getConnectorIdentifier(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());
    return connectorInfoDTO.isPresent() ? connectorInfoDTO.get().getName() : null;
  }
}
