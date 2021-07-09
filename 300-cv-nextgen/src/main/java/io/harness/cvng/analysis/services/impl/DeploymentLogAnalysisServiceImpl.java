package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.LogsAnalysisSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.Cluster;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ResultSummary;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis.DeploymentLogAnalysisKeys;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.utils.CVNGObjectUtils;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
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
      String accountId, String verificationJobInstanceId, String hostName) {
    List<DeploymentLogAnalysis> latestDeploymentLogAnalysis =
        getLatestDeploymentLogAnalysis(accountId, verificationJobInstanceId);
    if (isEmpty(latestDeploymentLogAnalysis)) {
      return Collections.emptyList();
    }

    boolean shouldFilterByHostName = StringUtils.isNotBlank(hostName);

    List<LogAnalysisClusterChartDTO> allClusters = new ArrayList<>();

    for (DeploymentLogAnalysis deploymentLogAnalysis : latestDeploymentLogAnalysis) {
      List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOList =
          getLogAnalysisClusterChartList(deploymentLogAnalysis, hostName, shouldFilterByHostName);

      if (shouldFilterByHostName) {
        logAnalysisClusterChartDTOList.forEach(logAnalysisClusterChartDTO
            -> deploymentLogAnalysis.getHostSummaries()
                   .stream()
                   .filter(hostSummary -> hostName.equals(hostSummary.getHost()))
                   .findFirst()
                   .ifPresent(hostSummary
                       -> updateClusterChartDTOWithRisk(logAnalysisClusterChartDTO, hostSummary.getResultSummary())));

      } else {
        logAnalysisClusterChartDTOList.forEach(logAnalysisClusterChartDTO
            -> updateClusterChartDTOWithRisk(logAnalysisClusterChartDTO, deploymentLogAnalysis.getResultSummary()));
      }
      allClusters.addAll(logAnalysisClusterChartDTOList);
    }

    return allClusters;
  }

  @Override
  public PageResponse<LogAnalysisClusterDTO> getLogAnalysisResult(
      String accountId, String verificationJobInstanceId, Integer label, int pageNumber, String hostName) {
    List<LogAnalysisClusterDTO> logAnalysisClusters =
        getLogAnalysisResult(accountId, verificationJobInstanceId, label, hostName);

    return formPageResponse(logAnalysisClusters, pageNumber, DEFAULT_PAGE_SIZE);
  }

  private List<LogAnalysisClusterDTO> getLogAnalysisResult(
      String accountId, String verificationJobInstanceId, Integer label, String hostName) {
    List<DeploymentLogAnalysis> latestDeploymentLogAnalysis =
        getLatestDeploymentLogAnalysis(accountId, verificationJobInstanceId);
    if (isEmpty(latestDeploymentLogAnalysis)) {
      return Collections.emptyList();
    }
    boolean shouldFilterByHostName = StringUtils.isNotBlank(hostName);
    List<LogAnalysisClusterDTO> logAnalysisClusters = new ArrayList<>();

    for (DeploymentLogAnalysis deploymentLogAnalysis : latestDeploymentLogAnalysis) {
      deploymentLogAnalysis.getResultSummary().setLabelToControlDataMap();
      if (shouldFilterByHostName) {
        logAnalysisClusters.addAll(getHostSpecificLogAnalysisClusters(deploymentLogAnalysis, label, hostName));
      } else {
        logAnalysisClusters.addAll(getOverallLogAnalysisClusters(deploymentLogAnalysis, label));
      }
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
      List<LogAnalysisClusterDTO> logAnalysisClusters =
          getLogAnalysisResult(accountId, verificationJobInstanceId, null, "");
      int anomClusters = 0, totalClusters = 0;
      for (LogAnalysisClusterDTO logAnalysisClusterDTO : logAnalysisClusters) {
        if (logAnalysisClusterDTO.getRisk().isGreaterThan(Risk.LOW)) {
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
      return Optional.of(recentHighestDeploymentLogAnalysis.getResultSummary().getRisk());
    }
  }

  private PageResponse<LogAnalysisClusterDTO> formPageResponse(

      List<LogAnalysisClusterDTO> logAnalysisClusters, int pageNumber, int size) {
    List<LogAnalysisClusterDTO> returnList = new ArrayList<>();

    int startIndex = pageNumber * size;
    Iterator<LogAnalysisClusterDTO> iterator = logAnalysisClusters.iterator();
    int i = 0;
    while (iterator.hasNext()) {
      LogAnalysisClusterDTO logAnalysisClusterDTO = iterator.next();
      if (i >= startIndex && returnList.size() < size) {
        returnList.add(logAnalysisClusterDTO);
      }
      i++;
    }

    return PageResponse.<LogAnalysisClusterDTO>builder()
        .pageSize(size)
        .pageIndex(pageNumber)
        .totalPages(logAnalysisClusters.size() / size)
        .totalItems(logAnalysisClusters.size())
        .content(returnList)
        .build();
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
      String accountId, String verificationJobInstanceId) {
    Set<String> verificationTaskIds =
        verificationTaskService.maybeGetVerificationTaskIds(accountId, verificationJobInstanceId);
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

  private LogAnalysisClusterChartDTO updateClusterChartDTOWithRisk(
      LogAnalysisClusterChartDTO logAnalysisClusterChartDTO, ResultSummary resultSummary) {
    resultSummary.getTestClusterSummaries()
        .stream()
        .filter(clusterSummary -> logAnalysisClusterChartDTO.getLabel() == clusterSummary.getLabel())
        .findFirst()
        .ifPresent(clusterSummary -> logAnalysisClusterChartDTO.setRisk(clusterSummary.getRisk()));
    return logAnalysisClusterChartDTO;
  }

  private List<LogAnalysisClusterChartDTO> getLogAnalysisClusterChartList(
      DeploymentLogAnalysis deploymentLogAnalysis, String hostName, boolean shouldFilterByHostName) {
    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOList = new ArrayList<>();
    deploymentLogAnalysis.getClusters().forEach(cluster
        -> deploymentLogAnalysis.getClusterCoordinates()
               .stream()
               .filter(clusterCoordinates
                   -> (shouldFilterByHostName ? clusterCoordinates.getHost().equals(hostName) : Boolean.TRUE)
                       && (cluster.getLabel() == clusterCoordinates.getLabel()))
               .forEach(clusterCoordinates
                   -> logAnalysisClusterChartDTOList.add(LogAnalysisClusterChartDTO.builder()
                                                             .label(cluster.getLabel())
                                                             .text(cluster.getText())
                                                             .hostName(clusterCoordinates.getHost())
                                                             .x(clusterCoordinates.getX())
                                                             .y(clusterCoordinates.getY())
                                                             .build())));
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
                                           .risk(clusterSummary.getRisk())
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
