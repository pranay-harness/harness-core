package io.harness.cvng.analysis.services.impl;

import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.CanaryDeploymentAdditionalInfo;
import io.harness.cvng.analysis.beans.CanaryDeploymentAdditionalInfo.HostSummaryInfo;
import io.harness.cvng.analysis.beans.CanaryDeploymentAdditionalInfo.TrafficSplitPercentage;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.core.beans.LoadTestAdditionalInfo;
import io.harness.cvng.core.beans.LoadTestAdditionalInfo.LoadTestAdditionalInfoBuilder;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.services.api.HostRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO.TimeSeriesRisk;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DeploymentAnalysisServiceImpl implements DeploymentAnalysisService {
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private HostRecordService hostRecordService;
  @Inject private ActivityService activityService;

  @Override
  public Optional<Double> getLatestRiskScore(String accountId, String verificationJobInstanceId) {
    Optional<Double> recentHighestTimeSeriesRiskScore =
        deploymentTimeSeriesAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId);
    Optional<Double> latestLogRiskScore =
        deploymentLogAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId);
    if (recentHighestTimeSeriesRiskScore.isPresent() && latestLogRiskScore.isPresent()) {
      return Optional.of(Math.max(recentHighestTimeSeriesRiskScore.get(), latestLogRiskScore.get()));
    } else if (recentHighestTimeSeriesRiskScore.isPresent()) {
      return recentHighestTimeSeriesRiskScore;
    } else if (latestLogRiskScore.isPresent()) {
      return latestLogRiskScore;
    } else {
      return Optional.empty();
    }
  }

  @Override
  public LoadTestAdditionalInfo getLoadTestAdditionalInfo(
      String accountId, VerificationJobInstance verificationJobInstance) {
    LoadTestAdditionalInfoBuilder loadTestSummaryBuilder = LoadTestAdditionalInfo.builder();
    String currentDeploymentTag =
        activityService.getDeploymentTagFromActivity(accountId, verificationJobInstance.getUuid());
    loadTestSummaryBuilder.currentDeploymentTag(currentDeploymentTag)
        .currentStartTime(verificationJobInstance.getStartTime().toEpochMilli());
    TestVerificationJob testVerificationJob = (TestVerificationJob) verificationJobInstance.getResolvedJob();
    String baselineVerificationJobInstanceId = testVerificationJob.getBaselineVerificationJobInstanceId();
    if (baselineVerificationJobInstanceId != null) {
      VerificationJobInstance baselineVerificationJobInstance =
          verificationJobInstanceService.getVerificationJobInstance(baselineVerificationJobInstanceId);
      String baselineDeploymentTag =
          activityService.getDeploymentTagFromActivity(accountId, baselineVerificationJobInstance.getUuid());
      loadTestSummaryBuilder.baselineDeploymentTag(baselineDeploymentTag)
          .baselineStartTime(baselineVerificationJobInstance.getStartTime().toEpochMilli());
    }
    return loadTestSummaryBuilder.build();
  }

  @Override
  public CanaryDeploymentAdditionalInfo getCanaryDeploymentAdditionalInfo(
      String accountId, VerificationJobInstance verificationJobInstance) {
    Optional<TimeRange> preDeploymentTimeRange =
        verificationJobInstanceService.getPreDeploymentTimeRange(verificationJobInstance.getUuid());

    Set<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstance.getUuid());

    Set<String> preDeploymentHosts = Collections.emptySet();
    if (preDeploymentTimeRange.isPresent()) {
      preDeploymentHosts = getPreDeploymentHosts(verificationTaskIds, preDeploymentTimeRange.get());
    }
    // TODO: need to use all latest DeploymentTimeSeriesAnalysis
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis =
        deploymentTimeSeriesAnalysisService.getLatestDeploymentTimeSeriesAnalysis(
            accountId, verificationJobInstance.getUuid());
    // TODO: need to use all latest DeploymentLogAnalysis
    DeploymentLogAnalysis deploymentLogAnalysis =
        deploymentLogAnalysisService.getLatestDeploymentLogAnalysis(accountId, verificationJobInstance.getUuid());

    CanaryDeploymentAdditionalInfo canaryDeploymentAdditionalInfo = getDeploymentVerificationHostInfoFromAnalyses(
        preDeploymentHosts, deploymentTimeSeriesAnalysis, deploymentLogAnalysis);
    updateHostInfoWithAnomalousCount(
        canaryDeploymentAdditionalInfo, deploymentTimeSeriesAnalysis, deploymentLogAnalysis);

    canaryDeploymentAdditionalInfo.setTrafficSplitPercentage(
        getTrafficSplitPercentage((CanaryVerificationJob) verificationJobInstance.getResolvedJob()));

    return canaryDeploymentAdditionalInfo;
  }

  private Set<String> getPreDeploymentHosts(Set<String> verificationTaskIds, TimeRange preDeploymentTimeRange) {
    Set<String> preDeploymentHosts = new HashSet<>();
    verificationTaskIds.forEach(verificationTaskId
        -> preDeploymentHosts.addAll(hostRecordService.get(
            verificationTaskId, preDeploymentTimeRange.getStartTime(), preDeploymentTimeRange.getEndTime())));
    return preDeploymentHosts;
  }

  private CanaryDeploymentAdditionalInfo getDeploymentVerificationHostInfoFromAnalyses(Set<String> preDeploymentHosts,
      DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis, DeploymentLogAnalysis deploymentLogAnalysis) {
    Set<HostSummaryInfo> primary = new HashSet<>();
    Set<HostSummaryInfo> canary = new HashSet<>();

    if (deploymentTimeSeriesAnalysis != null) {
      deploymentTimeSeriesAnalysis.getHostSummaries().forEach(hostInfo -> {
        HostSummaryInfo hostSummaryInfo = HostSummaryInfo.builder()
                                              .hostName(hostInfo.getHostName())
                                              .riskScore(TimeSeriesRisk.getRiskFromScore(hostInfo.getRisk()))
                                              .build();

        if (hostInfo.isPrimary()) {
          primary.add(hostSummaryInfo);
        }
        if (hostInfo.isCanary()) {
          canary.add(hostSummaryInfo);
        }
      });
    }
    if (deploymentLogAnalysis != null && deploymentLogAnalysis.getHostSummaries() != null) {
      canary.addAll(deploymentLogAnalysis.getHostSummaries()
                        .stream()
                        .map(hostSummary
                            -> HostSummaryInfo.builder()
                                   .hostName(hostSummary.getHost())
                                   .riskScore(TimeSeriesRisk.getRiskFromScore(hostSummary.getResultSummary().getRisk()))
                                   .build())
                        .collect(Collectors.toSet()));
    }

    //    Since Deployment Log Analysis only contains information about canary nodes, we get the list of primary nodes
    //     from HostRecordService. There might be a case when there is a primary node that is only there for Log
    //    Analysis and in that case we should add it to the existing ones which we collected from Time Series Analysis
    preDeploymentHosts.forEach(hostName -> {
      HostSummaryInfo hostSummaryInfo = HostSummaryInfo.builder().hostName(hostName).build();
      if (!primary.contains(hostSummaryInfo)) {
        primary.add(hostSummaryInfo);
      }
    });

    primary.forEach(hostSummaryInfo -> hostSummaryInfo.setRiskScore(null));
    return CanaryDeploymentAdditionalInfo.builder().primary(primary).canary(canary).build();
  }

  private CanaryDeploymentAdditionalInfo updateHostInfoWithAnomalousCount(
      CanaryDeploymentAdditionalInfo canaryDeploymentAdditionalInfo,
      DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis, DeploymentLogAnalysis deploymentLogAnalysis) {
    // improvised canary case
    if (canaryDeploymentAdditionalInfo.getCanary().isEmpty()) {
      canaryDeploymentAdditionalInfo.setCanary(canaryDeploymentAdditionalInfo.getPrimary());
    }

    canaryDeploymentAdditionalInfo.getCanary().forEach(hostSummaryInfo -> {
      int anomalousMetricsCount[] = new int[] {0};
      int anomalousLogClustersCount[] = new int[] {0};
      if (deploymentTimeSeriesAnalysis != null) {
        deploymentTimeSeriesAnalysis.getTransactionMetricSummaries().forEach(transactionMetricHostData
            -> anomalousMetricsCount[0] +=
            transactionMetricHostData.getHostData()
                .stream()
                .filter(hostData
                    -> hostData.getHostName().get().equals(hostSummaryInfo.getHostName()) && hostData.getRisk() >= 1)
                .count());
      }
      if (deploymentLogAnalysis != null && deploymentLogAnalysis.getHostSummaries() != null) {
        deploymentLogAnalysis.getHostSummaries()
            .stream()
            .filter(hostSummary -> hostSummary.getHost().equals(hostSummaryInfo.getHostName()))
            .forEach(hostSummary
                -> anomalousLogClustersCount[0] += hostSummary.getResultSummary()
                                                       .getTestClusterSummaries()
                                                       .stream()
                                                       .filter(clusterSummary -> clusterSummary.getRisk() >= 1)
                                                       .count());
      }
      hostSummaryInfo.setAnomalousMetricsCount(anomalousMetricsCount[0]);
      hostSummaryInfo.setAnomalousLogClustersCount(anomalousLogClustersCount[0]);
    });
    return canaryDeploymentAdditionalInfo;
  }

  private TrafficSplitPercentage getTrafficSplitPercentage(CanaryVerificationJob canaryVerificationJob) {
    Integer trafficSplitPercentage = canaryVerificationJob.getTrafficSplitPercentage();
    if (trafficSplitPercentage != null) {
      return TrafficSplitPercentage.builder()
          .preDeploymentPercentage(trafficSplitPercentage)
          .postDeploymentPercentage(100 - trafficSplitPercentage)
          .build();
    }
    return null;
  }
}
