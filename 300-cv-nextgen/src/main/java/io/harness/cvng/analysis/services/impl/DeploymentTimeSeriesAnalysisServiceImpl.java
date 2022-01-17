/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.TimeSeriesAnalysisSummary;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.beans.TransactionMetricInfo;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis.DeploymentTimeSeriesAnalysisKeys;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentTimeSeriesAnalysisFilter;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.entities.VerificationTask.DeploymentInfo;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.utils.CVNGObjectUtils;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.persistence.HPersistence;
import io.harness.serializer.JsonUtils;
import io.harness.utils.PageUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Sort;

public class DeploymentTimeSeriesAnalysisServiceImpl implements DeploymentTimeSeriesAnalysisService {
  public static final int DEFAULT_PAGE_SIZE = 10;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private NextGenService nextGenService;

  @Override
  public void save(DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis) {
    hPersistence.save(deploymentTimeSeriesAnalysis);
  }

  @Override
  public TransactionMetricInfoSummaryPageDTO getMetrics(String accountId, String verificationJobInstanceId,
      DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter, PageParams pageParams) {
    VerificationJobInstance verificationJobInstance =
        verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
    List<DeploymentTimeSeriesAnalysis> latestDeploymentTimeSeriesAnalysis =
        getLatestDeploymentTimeSeriesAnalysis(accountId, verificationJobInstanceId, deploymentTimeSeriesAnalysisFilter);
    if (isEmpty(latestDeploymentTimeSeriesAnalysis)) {
      return TransactionMetricInfoSummaryPageDTO.builder()
          .pageResponse(PageUtils.offsetAndLimit(Collections.emptyList(), pageParams.getPage(), pageParams.getSize()))
          .build();
    }
    TimeRange deploymentTimeRange = TimeRange.builder()
                                        .startTime(verificationJobInstance.getStartTime())
                                        .endTime(latestDeploymentTimeSeriesAnalysis.get(0).getEndTime())
                                        .build();
    List<TransactionMetricInfo> transactionMetricInfoList =
        getMetrics(accountId, verificationJobInstanceId, deploymentTimeSeriesAnalysisFilter);

    if (deploymentTimeSeriesAnalysisFilter.filterByFilter()) {
      transactionMetricInfoList =
          transactionMetricInfoList.stream()
              .filter(transactionMetricInfo
                  -> transactionMetricInfo.getTransactionMetric().getMetricName().toLowerCase().contains(
                         deploymentTimeSeriesAnalysisFilter.getFilter().toLowerCase())
                      || transactionMetricInfo.getTransactionMetric().getTransactionName().toLowerCase().contains(
                          deploymentTimeSeriesAnalysisFilter.getFilter().toLowerCase()))
              .collect(Collectors.toList());
    }

    return TransactionMetricInfoSummaryPageDTO.builder()
        .pageResponse(PageUtils.offsetAndLimit(transactionMetricInfoList, pageParams.getPage(), pageParams.getSize()))
        .deploymentTimeRange(deploymentTimeRange)
        .deploymentStartTime(deploymentTimeRange.getStartTime().toEpochMilli())
        .deploymentEndTime(deploymentTimeRange.getEndTime().toEpochMilli())
        .build();
  }

  @Override
  public TimeSeriesAnalysisSummary getAnalysisSummary(List<String> verificationJobInstanceIds) {
    Preconditions.checkNotNull(
        verificationJobInstanceIds, "Missing verificationJobInstanceIds when looking for summary");
    List<Integer> anomMetricCounts = new ArrayList<>();
    List<Integer> totalMetricCounts = new ArrayList<>();
    verificationJobInstanceIds.forEach(verificationJobInstanceId -> {
      VerificationJobInstance verificationJobInstance =
          verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId);
      List<TransactionMetricInfo> transactionMetricInfoList = getMetrics(verificationJobInstance.getAccountId(),
          verificationJobInstanceId, DeploymentTimeSeriesAnalysisFilter.builder().build());
      int numAnomMetrics = 0, totalMetrics = 0;
      for (TransactionMetricInfo transactionMetricInfo : transactionMetricInfoList) {
        if (transactionMetricInfo.getTransactionMetric().getRisk().isGreaterThan(Risk.HEALTHY)) {
          numAnomMetrics++;
        }
        totalMetrics++;
      }
      anomMetricCounts.add(numAnomMetrics);
      totalMetricCounts.add(totalMetrics);
    });

    return TimeSeriesAnalysisSummary.builder()
        .numAnomMetrics(anomMetricCounts.stream().mapToInt(Integer::intValue).sum())
        .totalNumMetrics(totalMetricCounts.stream().mapToInt(Integer::intValue).sum())
        .build();
  }

  @Override
  public String getTimeSeriesDemoTemplate(String verificationTaskId) {
    List<DeploymentTimeSeriesAnalysis> deploymentTimeSeriesAnalyses =
        hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class)
            .filter(DeploymentTimeSeriesAnalysisKeys.verificationTaskId, verificationTaskId)
            .asList();
    return JsonUtils.asJson(deploymentTimeSeriesAnalyses);
  }

  @Override
  public void addDemoAnalysisData(String verificationTaskId, CVConfig cvConfig,
      VerificationJobInstance verificationJobInstance, String demoTemplatePath) {
    try {
      String template = Resources.toString(this.getClass().getResource(demoTemplatePath), Charsets.UTF_8);
      List<DeploymentTimeSeriesAnalysis> deploymentTimeSeriesAnalyses =
          JsonUtils.asObject(template, new TypeReference<List<DeploymentTimeSeriesAnalysis>>() {});
      Collections.sort(deploymentTimeSeriesAnalyses, Comparator.comparing(DeploymentTimeSeriesAnalysis::getStartTime));
      Instant lastStartTime = deploymentTimeSeriesAnalyses.get(0).getStartTime();
      int minute = 0;
      for (DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis : deploymentTimeSeriesAnalyses) {
        deploymentTimeSeriesAnalysis.setVerificationTaskId(verificationTaskId);
        if (!lastStartTime.equals(deploymentTimeSeriesAnalysis.getStartTime())) {
          lastStartTime = deploymentTimeSeriesAnalysis.getStartTime();
          minute++;
        }
        deploymentTimeSeriesAnalysis.setStartTime(
            verificationJobInstance.getStartTime().plus(Duration.ofMinutes(minute)));
        deploymentTimeSeriesAnalysis.setEndTime(
            deploymentTimeSeriesAnalysis.getStartTime().plus(Duration.ofMinutes(1)));
      }
      hPersistence.save(deploymentTimeSeriesAnalyses);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<TransactionMetricInfo> getMetrics(String accountId, String verificationJobInstanceId,
      DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter) {
    List<DeploymentTimeSeriesAnalysis> latestDeploymentTimeSeriesAnalysis =
        getLatestDeploymentTimeSeriesAnalysis(accountId, verificationJobInstanceId, deploymentTimeSeriesAnalysisFilter);

    if (isEmpty(latestDeploymentTimeSeriesAnalysis)) {
      return Collections.emptyList();
    }

    Set<TransactionMetricInfo> transactionMetricInfoSet = new HashSet();
    for (DeploymentTimeSeriesAnalysis timeSeriesAnalysis : latestDeploymentTimeSeriesAnalysis) {
      VerificationTask verificationTask = verificationTaskService.get(timeSeriesAnalysis.getVerificationTaskId());
      Preconditions.checkNotNull(verificationTask.getTaskInfo().getTaskType().equals(TaskType.DEPLOYMENT),
          "VerificationTask should be of Deployment type");
      CVConfig cvConfig = verificationJobInstanceService.getEmbeddedCVConfig(
          ((DeploymentInfo) verificationTask.getTaskInfo()).getCvConfigId(),
          ((DeploymentInfo) verificationTask.getTaskInfo()).getVerificationJobInstanceId());

      String connectorName = getConnectorName(cvConfig);
      DataSourceType dataSourceType = cvConfig.getType();

      timeSeriesAnalysis.getTransactionMetricSummaries()
          .stream()
          .filter(transactionMetricHostData
              -> filterAnomalousMetrics(transactionMetricHostData,
                  deploymentTimeSeriesAnalysisFilter.filterByHostName(),
                  deploymentTimeSeriesAnalysisFilter.isAnomalous()))
          .forEach(transactionMetricHostData -> {
            Map<Risk, Integer> nodeCountByRiskStatusMap = new HashMap<>();
            SortedSet<DeploymentTimeSeriesAnalysisDTO.HostData> nodeDataSet = new TreeSet();
            transactionMetricHostData.getHostData()
                .stream()
                .filter(hostData
                    -> filterHostData(hostData, deploymentTimeSeriesAnalysisFilter.getHostName(),
                        deploymentTimeSeriesAnalysisFilter.isAnomalous()))
                .forEach(hostData -> {
                  nodeDataSet.add(hostData);
                  Risk risk = hostData.getRisk();
                  nodeCountByRiskStatusMap.put(risk, nodeCountByRiskStatusMap.getOrDefault(risk, 0) + 1);
                });
            TransactionMetricInfo transactionMetricInfo =
                TransactionMetricInfo.builder()
                    .transactionMetric(createTransactionMetric(transactionMetricHostData))
                    .connectorName(connectorName)
                    .dataSourceType(dataSourceType)
                    .nodes(nodeDataSet)
                    .nodeCountByRiskStatusMap(nodeCountByRiskStatusMap)
                    .build();
            if (isNotEmpty(nodeDataSet)) {
              transactionMetricInfoSet.add(transactionMetricInfo);
            }
          });
    }

    List<TransactionMetricInfo> transactionMetricInfoList = new ArrayList<>(transactionMetricInfoSet);
    transactionMetricInfoList.sort(
        (d1, d2) -> Double.compare(d2.getTransactionMetric().getScore(), d1.getTransactionMetric().getScore()));
    return transactionMetricInfoList;
  }

  private boolean filterHostData(
      DeploymentTimeSeriesAnalysisDTO.HostData hostData, String hostName, boolean anomalousMetricsOnly) {
    if (StringUtils.isBlank(hostName)) {
      return true;
    } else if (hostData.getHostName().isPresent()) {
      return hostName.equals(hostData.getHostName().get()) && (!anomalousMetricsOnly || hostData.isAnomalous());
    } else {
      return false;
    }
  }

  private boolean filterAnomalousMetrics(
      DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData, boolean hostFilterExists,
      boolean anomalousMetricsOnly) {
    if (hostFilterExists) {
      return true; // need to filter at host data level.
    }
    return !anomalousMetricsOnly || transactionMetricHostData.isAnomalous();
  }

  private TransactionMetricInfo.TransactionMetric createTransactionMetric(
      DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData) {
    return TransactionMetricInfo.TransactionMetric.builder()
        .transactionName(transactionMetricHostData.getTransactionName())
        .metricName(transactionMetricHostData.getMetricName())
        .score(transactionMetricHostData.getScore())
        .risk(transactionMetricHostData.getRisk())
        .build();
  }

  @Override
  public List<DeploymentTimeSeriesAnalysis> getAnalysisResults(String verificationTaskId) {
    return hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class, excludeAuthority)
        .filter(DeploymentTimeSeriesAnalysisKeys.verificationTaskId, verificationTaskId)
        .asList();
  }

  @Override
  public Optional<Risk> getRecentHighestRiskScore(String accountId, String verificationJobInstanceId) {
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis =
        getRecentHighestDeploymentTimeSeriesAnalysis(accountId, verificationJobInstanceId);
    if (deploymentTimeSeriesAnalysis == null) {
      return Optional.empty();
    } else {
      return Optional.of(deploymentTimeSeriesAnalysis.getRisk());
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
  public List<DeploymentTimeSeriesAnalysis> getLatestDeploymentTimeSeriesAnalysis(String accountId,
      String verificationJobInstanceId, DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter) {
    Set<String> verificationTaskIds =
        verificationTaskService.maybeGetVerificationTaskIds(accountId, verificationJobInstanceId);

    if (deploymentTimeSeriesAnalysisFilter.filterByHealthSourceIdentifiers()) {
      List<String> cvConfigIds = verificationJobInstanceService.getCVConfigIdsForVerificationJobInstance(
          verificationJobInstanceId, deploymentTimeSeriesAnalysisFilter.getHealthSourceIdentifiers());
      verificationTaskIds =
          verificationTaskIds.stream()
              .filter(
                  verificationTaskId -> cvConfigIds.contains(verificationTaskService.getCVConfigId(verificationTaskId)))
              .collect(Collectors.toSet());
    }

    List<DeploymentTimeSeriesAnalysis> timeSeriesAnalyses = new ArrayList<>();
    verificationTaskIds.forEach(taskId -> {
      DeploymentTimeSeriesAnalysis analysis = hPersistence.createQuery(DeploymentTimeSeriesAnalysis.class)
                                                  .filter(DeploymentTimeSeriesAnalysisKeys.verificationTaskId, taskId)
                                                  .order(Sort.descending(DeploymentTimeSeriesAnalysisKeys.startTime))
                                                  .get();
      if (analysis != null) {
        timeSeriesAnalyses.add(analysis);
      }
    });
    return timeSeriesAnalyses;
  }

  @Nullable
  private String getConnectorName(CVConfig cvConfig) {
    Preconditions.checkNotNull(cvConfig, "CVConfig should not be null");
    Optional<ConnectorInfoDTO> connectorInfoDTO = nextGenService.get(cvConfig.getAccountId(),
        cvConfig.getConnectorIdentifier(), cvConfig.getOrgIdentifier(), cvConfig.getProjectIdentifier());
    return connectorInfoDTO.isPresent() ? connectorInfoDTO.get().getName() : null;
  }
}
