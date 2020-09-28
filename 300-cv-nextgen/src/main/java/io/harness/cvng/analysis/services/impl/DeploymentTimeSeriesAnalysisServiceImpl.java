package io.harness.cvng.analysis.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfo;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis.DeploymentTimeSeriesAnalysisKeys;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;

public class DeploymentTimeSeriesAnalysisServiceImpl implements DeploymentTimeSeriesAnalysisService {
  public static final int DEFAULT_PAGE_SIZE = 10;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;

  @Override
  public void save(DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis) {
    hPersistence.save(deploymentTimeSeriesAnalysis);
  }

  @Override
  public TransactionMetricInfoSummaryPageDTO getMetrics(String accountId, String verificationJobInstanceId,
      boolean anomalousMetricsOnly, String hostName, int pageNumber) {
    Set<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId);

    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);

    DeploymentTimeSeriesAnalysis latestDeploymentTimeSeriesAnalysis =
        getLatestDeploymentTimeSeriesAnalysis(verificationTaskIds);

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

    latestDeploymentTimeSeriesAnalysis.getTransactionMetricSummaries()
        .stream()
        .filter(transactionMetricHostData -> filterAnomalousMetrics(transactionMetricHostData, anomalousMetricsOnly))
        .forEach(transactionMetricHostData -> {
          TransactionMetricInfo transactionMetricInfo =
              TransactionMetricInfo.builder()
                  .transactionMetric(createTransactionMetric(transactionMetricHostData))
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
    } else {
      return hostName.equals(hostData.getHostName());
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

  private DeploymentTimeSeriesAnalysis getLatestDeploymentTimeSeriesAnalysis(Set<String> verificationTaskIds) {
    return hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class)
        .field(DeploymentTimeSeriesAnalysisKeys.verificationTaskId)
        .in(verificationTaskIds)
        .order(Sort.descending(DeploymentTimeSeriesAnalysisKeys.startTime))
        .get();
  }
}
