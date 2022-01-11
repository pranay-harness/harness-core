/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.LogsAnalysisSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.Cluster;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ResultSummary;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis.DeploymentLogAnalysisKeys;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentLogAnalysisFilter;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.utils.CVNGObjectUtils;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.ng.beans.PageResponse;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.mongodb.morphia.query.Sort;

public class DeploymentLogAnalysisServiceImpl implements DeploymentLogAnalysisService {
  public static final int DEFAULT_PAGE_SIZE = 10;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Override
  public void save(DeploymentLogAnalysis deploymentLogAnalysis) {
    hPersistence.save(deploymentLogAnalysis);
  }

  @Override
  public List<DeploymentLogAnalysis> getAnalysisResults(String verificationTaskId) {
    return hPersistence.createQuery(DeploymentLogAnalysis.class)
        .filter(DeploymentLogAnalysisKeys.verificationTaskId, verificationTaskId)
        .asList();
  }

  @Override
  public List<LogAnalysisClusterChartDTO> getLogAnalysisClusters(
      String accountId, String verificationJobInstanceId, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter) {
    List<DeploymentLogAnalysis> latestDeploymentLogAnalysis =
        getLatestDeploymentLogAnalysis(accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    if (isEmpty(latestDeploymentLogAnalysis)) {
      return Collections.emptyList();
    }

    List<LogAnalysisClusterChartDTO> allClusters = new ArrayList<>();
    for (DeploymentLogAnalysis deploymentLogAnalysis : latestDeploymentLogAnalysis) {
      List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOList =
          getLogAnalysisClusterChartList(deploymentLogAnalysis, deploymentLogAnalysisFilter.getHostName());

      Map<Integer, ClusterSummary> clusterSummaryMap = new HashMap<>();
      deploymentLogAnalysis.getResultSummary().getTestClusterSummaries().forEach(
          clusterSummary -> { clusterSummaryMap.put(clusterSummary.getLabel(), clusterSummary); });

      logAnalysisClusterChartDTOList.forEach(logAnalysisClusterChartDTO -> {
        if (clusterSummaryMap.containsKey(logAnalysisClusterChartDTO.getLabel())
            && (!deploymentLogAnalysisFilter.filterByClusterType()
                || deploymentLogAnalysisFilter.getClusterTypes().contains(
                    clusterSummaryMap.get(logAnalysisClusterChartDTO.getLabel()).getClusterType()))) {
          logAnalysisClusterChartDTO.setRisk(
              clusterSummaryMap.get(logAnalysisClusterChartDTO.getLabel()).getRiskLevel());
          logAnalysisClusterChartDTO.setClusterType(
              clusterSummaryMap.get(logAnalysisClusterChartDTO.getLabel()).getClusterType());
          allClusters.add(logAnalysisClusterChartDTO);
        }
      });
    }
    return allClusters;
  }

  @Override
  public PageResponse<LogAnalysisClusterDTO> getLogAnalysisResult(String accountId, String verificationJobInstanceId,
      Integer label, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter, PageParams pageParams) {
    List<LogAnalysisClusterDTO> logAnalysisClusters =
        getLogAnalysisResult(accountId, verificationJobInstanceId, label, deploymentLogAnalysisFilter);

    return PageUtils.offsetAndLimit(logAnalysisClusters, pageParams.getPage(), pageParams.getSize());
  }

  private List<LogAnalysisClusterDTO> getLogAnalysisResult(String accountId, String verificationJobInstanceId,
      Integer label, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter) {
    List<DeploymentLogAnalysis> latestDeploymentLogAnalysis =
        getLatestDeploymentLogAnalysis(accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    if (isEmpty(latestDeploymentLogAnalysis)) {
      return Collections.emptyList();
    }
    boolean shouldFilterByHostName = deploymentLogAnalysisFilter.filterByHostName();
    List<LogAnalysisClusterDTO> logAnalysisClusters = new ArrayList<>();

    for (DeploymentLogAnalysis deploymentLogAnalysis : latestDeploymentLogAnalysis) {
      deploymentLogAnalysis.getResultSummary().setLabelToControlDataMap();
      if (shouldFilterByHostName) {
        logAnalysisClusters.addAll(getHostSpecificLogAnalysisClusters(
            deploymentLogAnalysis, label, deploymentLogAnalysisFilter.getHostName()));
      } else {
        logAnalysisClusters.addAll(getOverallLogAnalysisClusters(deploymentLogAnalysis, label));
      }
    }
    if (deploymentLogAnalysisFilter.filterByClusterType()) {
      logAnalysisClusters =
          logAnalysisClusters.stream()
              .filter(
                  logAnalysis -> deploymentLogAnalysisFilter.getClusterTypes().contains(logAnalysis.getClusterType()))
              .collect(Collectors.toList());
    }
    logAnalysisClusters.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
    return logAnalysisClusters;
  }

  @Override
  public LogsAnalysisSummary getAnalysisSummary(String accountId, List<String> verificationJobInstanceIds) {
    List<Integer> anomClusterCounts = new ArrayList<>();
    List<Integer> totalClusterCounts = new ArrayList<>();

    Preconditions.checkNotNull(
        verificationJobInstanceIds, "Missing verificationJobInstanceIds when looking for summary");
    verificationJobInstanceIds.forEach(verificationJobInstanceId -> {
      List<LogAnalysisClusterDTO> logAnalysisClusters = getLogAnalysisResult(
          accountId, verificationJobInstanceId, null, DeploymentLogAnalysisFilter.builder().build());
      int anomClusters = 0, totalClusters = 0;
      for (LogAnalysisClusterDTO logAnalysisClusterDTO : logAnalysisClusters) {
        if (logAnalysisClusterDTO.getRisk().isGreaterThan(Risk.HEALTHY)) {
          anomClusters++;
        }
        totalClusters++;
      }
      anomClusterCounts.add(anomClusters);
      totalClusterCounts.add(totalClusters);
    });
    return LogsAnalysisSummary.builder()
        .anomalousClusterCount(anomClusterCounts.stream().mapToInt(Integer::intValue).sum())
        .totalClusterCount(totalClusterCounts.stream().mapToInt(Integer::intValue).sum())
        .build();
  }

  @Override
  public Optional<Risk> getRecentHighestRiskScore(String accountId, String verificationJobInstanceId) {
    DeploymentLogAnalysis recentHighestDeploymentLogAnalysis =
        getRecentHighestDeploymentLogAnalysis(accountId, verificationJobInstanceId);
    if (recentHighestDeploymentLogAnalysis == null) {
      return Optional.empty();
    } else {
      return Optional.of(recentHighestDeploymentLogAnalysis.getResultSummary().getRiskLevel());
    }
  }

  @Override
  @Nullable
  public DeploymentLogAnalysis getRecentHighestDeploymentLogAnalysis(
      String accountId, String verificationJobInstanceId) {
    Set<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId);
    DeploymentLogAnalysis max = null;
    for (String verificationTaskId : verificationTaskIds) {
      DeploymentLogAnalysis deploymentLogAnalysis =
          hPersistence.createQuery(DeploymentLogAnalysis.class)
              .filter(DeploymentLogAnalysisKeys.verificationTaskId, verificationTaskId)
              .order(Sort.descending(DeploymentLogAnalysisKeys.startTime))
              .get();
      max = CVNGObjectUtils.max(max, deploymentLogAnalysis,
          Comparator.comparingDouble(logAnalysis -> logAnalysis.getResultSummary().getScore()));
    }
    return max;
  }

  @Override
  public List<DeploymentLogAnalysis> getLatestDeploymentLogAnalysis(
      String accountId, String verificationJobInstanceId, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter) {
    Set<String> verificationTaskIds =
        verificationTaskService.maybeGetVerificationTaskIds(accountId, verificationJobInstanceId);
    if (deploymentLogAnalysisFilter.filterByHealthSourceIdentifiers()) {
      List<String> cvConfigIds = verificationJobInstanceService.getCVConfigIdsForVerificationJobInstance(
          verificationJobInstanceId, deploymentLogAnalysisFilter.getHealthSourceIdentifiers());
      verificationTaskIds =
          verificationTaskIds.stream()
              .filter(
                  verificationTaskId -> cvConfigIds.contains(verificationTaskService.getCVConfigId(verificationTaskId)))
              .collect(Collectors.toSet());
    }

    List<DeploymentLogAnalysis> deploymentLogAnalyses = new ArrayList<>();
    for (String taskId : verificationTaskIds) {
      DeploymentLogAnalysis logAnalysis = hPersistence.createQuery(DeploymentLogAnalysis.class)
                                              .filter(DeploymentLogAnalysisKeys.verificationTaskId, taskId)
                                              .order(Sort.descending(DeploymentLogAnalysisKeys.startTime))
                                              .get();
      if (logAnalysis != null) {
        deploymentLogAnalyses.add(logAnalysis);
      }
    }
    return deploymentLogAnalyses;
  }

  @Override
  public String getLogDemoTemplate(String verificationTaskId) {
    List<DeploymentLogAnalysis> deploymentLogAnalyses =
        hPersistence.createQuery(DeploymentLogAnalysis.class)
            .filter(DeploymentLogAnalysisKeys.verificationTaskId, verificationTaskId)
            .asList();
    return JsonUtils.asJson(deploymentLogAnalyses);
  }

  @Override
  public void addDemoAnalysisData(String verificationTaskId, CVConfig cvConfig,
      VerificationJobInstance verificationJobInstance, String demoTemplatePath) {
    try {
      String template = Resources.toString(this.getClass().getResource(demoTemplatePath), Charsets.UTF_8);
      List<DeploymentLogAnalysis> deploymentLogAnalyses =
          JsonUtils.asObject(template, new TypeReference<List<DeploymentLogAnalysis>>() {});
      deploymentLogAnalyses.sort(Comparator.comparing(DeploymentLogAnalysis::getStartTime));
      Instant lastStartTime = deploymentLogAnalyses.get(0).getStartTime();
      int minute = 0;
      for (DeploymentLogAnalysis deploymentLogAnalysis : deploymentLogAnalyses) {
        deploymentLogAnalysis.setVerificationTaskId(verificationTaskId);
        if (!lastStartTime.equals(deploymentLogAnalysis.getStartTime())) {
          lastStartTime = deploymentLogAnalysis.getStartTime();
          minute++;
        }
        deploymentLogAnalysis.setStartTime(verificationJobInstance.getStartTime().plus(Duration.ofMinutes(minute)));
        deploymentLogAnalysis.setEndTime(deploymentLogAnalysis.getStartTime().plus(Duration.ofMinutes(1)));
      }
      hPersistence.save(deploymentLogAnalyses);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<LogAnalysisClusterChartDTO> getLogAnalysisClusterChartList(
      DeploymentLogAnalysis deploymentLogAnalysis, String hostName) {
    Map<Integer, Cluster> labelToClusterMap = new HashMap<>();
    deploymentLogAnalysis.getClusters().forEach(cluster -> labelToClusterMap.put(cluster.getLabel(), cluster));

    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOList = new ArrayList<>();
    deploymentLogAnalysis.getClusterCoordinates()
        .stream()
        .filter(clusterCoordinate -> hostName == null || clusterCoordinate.getHost().equals(hostName))
        .forEach(clusterCoordinate -> {
          Cluster cluster = labelToClusterMap.get(clusterCoordinate.getLabel());
          logAnalysisClusterChartDTOList.add(LogAnalysisClusterChartDTO.builder()
                                                 .label(cluster.getLabel())
                                                 .text(cluster.getText())
                                                 .hostName(clusterCoordinate.getHost())
                                                 .x(clusterCoordinate.getX())
                                                 .y(clusterCoordinate.getY())
                                                 .build());
        });
    return logAnalysisClusterChartDTOList;
  }

  private void addLogAnalysisClusterDTO(
      List<LogAnalysisClusterDTO> logAnalysisClusters, ResultSummary resultSummary, Cluster cluster) {
    resultSummary.getTestClusterSummaries()
        .stream()
        .filter(clusterSummary -> clusterSummary.getLabel() == cluster.getLabel())
        .forEach(clusterSummary
            -> logAnalysisClusters.add(LogAnalysisClusterDTO.builder()
                                           .label(cluster.getLabel())
                                           .message(cluster.getText())
                                           .risk(clusterSummary.getRiskLevel())
                                           .score(clusterSummary.getScore())
                                           .count(clusterSummary.getCount())
                                           .clusterType(clusterSummary.getClusterType())
                                           .controlFrequencyData(resultSummary.getControlData(cluster.getLabel()))
                                           .testFrequencyData(clusterSummary.getTestFrequencyData())
                                           .build()));
  }

  private List<LogAnalysisClusterDTO> getHostSpecificLogAnalysisClusters(
      DeploymentLogAnalysis deploymentLogAnalysis, Integer label, String hostName) {
    List<LogAnalysisClusterDTO> logAnalysisClusters = new ArrayList<>();
    deploymentLogAnalysis.getClusters()
        .stream()
        .filter(cluster -> label != null ? cluster.getLabel() == label : Boolean.TRUE)
        .forEach(cluster
            -> deploymentLogAnalysis.getHostSummaries()
                   .stream()
                   .filter(hostSummary -> hostSummary.getHost().equals(hostName))
                   .forEach(hostSummary
                       -> addLogAnalysisClusterDTO(logAnalysisClusters, hostSummary.getResultSummary(), cluster)));
    return logAnalysisClusters;
  }

  private List<LogAnalysisClusterDTO> getOverallLogAnalysisClusters(
      DeploymentLogAnalysis deploymentLogAnalysis, Integer label) {
    List<LogAnalysisClusterDTO> logAnalysisClusters = new ArrayList<>();
    deploymentLogAnalysis.getClusters()
        .stream()
        .filter(cluster -> label != null ? cluster.getLabel() == label : Boolean.TRUE)
        .forEach(cluster
            -> addLogAnalysisClusterDTO(logAnalysisClusters, deploymentLogAnalysis.getResultSummary(), cluster));
    return logAnalysisClusters;
  }
}
