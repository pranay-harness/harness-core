package io.harness.cvng.analysis.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis.DeploymentLogAnalysisKeys;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DeploymentLogAnalysisServiceImpl implements DeploymentLogAnalysisService {
  public static final int DEFAULT_PAGE_SIZE = 10;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationTaskService verificationTaskService;
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
  public List<LogAnalysisClusterChartDTO> getLogAnalysisClusters(String accountId, String verificationJobInstanceId) {
    Set<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId);

    DeploymentLogAnalysis latestDeploymentLogAnalysis = getLatestDeploymentLogAnalysis(verificationTaskIds);
    if (latestDeploymentLogAnalysis == null) {
      return Collections.emptyList();
    }
    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOList = new ArrayList();

    latestDeploymentLogAnalysis.getClusters().forEach(cluster
        -> latestDeploymentLogAnalysis.getResultSummary()
               .getTestClusterSummaries()
               .stream()
               .filter(clusterSummary -> cluster.getLabel() == clusterSummary.getLabel())
               .findFirst()
               .ifPresent(clusterSummary
                   -> logAnalysisClusterChartDTOList.add(LogAnalysisClusterChartDTO.builder()
                                                             .label(cluster.getLabel())
                                                             .x(cluster.getX())
                                                             .y(cluster.getY())
                                                             .text(cluster.getText())
                                                             .risk(clusterSummary.getRisk())
                                                             .build())));

    return logAnalysisClusterChartDTOList;
  }

  public PageResponse<LogAnalysisClusterDTO> getLogAnalysisResult(
      String accountId, String verificationJobInstanceId, Integer label, int pageNumber) {
    Set<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId);

    DeploymentLogAnalysis latestDeploymentLogAnalysis = getLatestDeploymentLogAnalysis(verificationTaskIds);
    if (latestDeploymentLogAnalysis == null) {
      return formPageResponse(Collections.emptyList(), pageNumber, DEFAULT_PAGE_SIZE);
    }

    List<LogAnalysisClusterDTO> logAnalysisClusters = new ArrayList();
    latestDeploymentLogAnalysis.getClusters()
        .stream()
        .filter(cluster -> label != null ? cluster.getLabel() == label : Boolean.TRUE)
        .forEach(cluster
            -> latestDeploymentLogAnalysis.getResultSummary()
                   .getTestClusterSummaries()
                   .stream()
                   .filter(clusterSummary -> cluster.getLabel() == clusterSummary.getLabel())
                   .findFirst()
                   .ifPresent(clusterSummary
                       -> logAnalysisClusters.add(LogAnalysisClusterDTO.builder()
                                                      .message(cluster.getText())
                                                      .label(cluster.getLabel())
                                                      .risk(clusterSummary.getRisk())
                                                      .score(clusterSummary.getScore())
                                                      .count(clusterSummary.getCount())
                                                      .clusterType(clusterSummary.getClusterType())
                                                      .controlFrequencyData(clusterSummary.getControlFrequencyData())
                                                      .testFrequencyData(clusterSummary.getTestFrequencyData())
                                                      .build())));

    Collections.sort(logAnalysisClusters, (a, b) -> Double.compare(b.getScore(), a.getScore()));

    return formPageResponse(logAnalysisClusters, pageNumber, DEFAULT_PAGE_SIZE);
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

  private DeploymentLogAnalysis getLatestDeploymentLogAnalysis(Set<String> verificationTaskIds) {
    return hPersistence.createQuery(DeploymentLogAnalysis.class)
        .field(DeploymentLogAnalysisKeys.verificationTaskId)
        .in(verificationTaskIds)
        .order(Sort.descending(DeploymentLogAnalysisKeys.startTime))
        .get();
  }
}
