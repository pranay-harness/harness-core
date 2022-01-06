/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.LogAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardTxnMetricAnalysisDataDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.ServiceGuardLogAnalysisTask;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesLearningEngineTask;
import io.harness.cvng.analysis.services.api.AnalysisService;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.RiskSummaryPopoverDTO;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.models.VerificationType;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AnalysisServiceImplTest extends CvNextGenTestBase {
  @Inject private AnalysisService analysisService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private LogAnalysisService logAnalysisService;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private LearningEngineTaskService learningEngineTaskService;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String serviceIdentifier;
  private Instant instant;
  private String splunkCVConfigId;
  private String appDynamicsCVConfigId;
  private String logVerificationTaskId;
  private String metricVerificationTaskId;
  @Before
  public void setUp() {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    instant = Instant.parse("2020-07-27T10:44:06.390Z");
    CVConfig cvConfig = cvConfigService.save(createSplunkCVConfig());
    splunkCVConfigId = cvConfig.getUuid();
    logVerificationTaskId =
        verificationTaskService.createLiveMonitoringVerificationTask(accountId, splunkCVConfigId, cvConfig.getType());
    cvConfig = cvConfigService.save(creatAppDynamicsCVConfig());
    appDynamicsCVConfigId = cvConfig.getUuid();
    metricVerificationTaskId = verificationTaskService.createLiveMonitoringVerificationTask(
        accountId, appDynamicsCVConfigId, cvConfig.getType());
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTop3AnalysisRisks_withoutLogs() throws IllegalAccessException {
    FieldUtils.writeField(timeSeriesAnalysisService, "heatMapService", mock(HeatMapService.class), true);
    LearningEngineTask learningEngineTask = createTimeseriesTask(metricVerificationTaskId);
    timeSeriesAnalysisService.saveAnalysis(learningEngineTask.getUuid(),
        buildServiceGuardMetricAnalysisDTO(
            Lists.newArrayList("m1", "m2", "m3", "m4"), Lists.newArrayList(.2, .4, .5, .6)));
    List<RiskSummaryPopoverDTO.AnalysisRisk> analysisRisks = analysisService.getTop3AnalysisRisks(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, instant.minus(Duration.ofMinutes(15)), instant);

    assertThat(analysisRisks).hasSize(3);
    for (int i = 0; i < analysisRisks.size(); i++) {
      assertThat(analysisRisks.get(i).getName()).isEqualTo("txn - m" + (2 + i));
      assertThat(analysisRisks.get(i).getRisk()).isEqualTo((int) ((0.4 + i * .1) * 100));
    }
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTop3AnalysisRisks_logsAndMetrics() throws IllegalAccessException {
    FieldUtils.writeField(timeSeriesAnalysisService, "heatMapService", mock(HeatMapService.class), true);
    FieldUtils.writeField(logAnalysisService, "heatMapService", mock(HeatMapService.class), true);
    LearningEngineTask learningEngineTask1 = createTimeseriesTask(metricVerificationTaskId);
    LearningEngineTask learningEngineTask2 =
        create(logVerificationTaskId, LearningEngineTaskType.SERVICE_GUARD_LOG_ANALYSIS);
    timeSeriesAnalysisService.saveAnalysis(learningEngineTask1.getUuid(),
        buildServiceGuardMetricAnalysisDTO(
            Lists.newArrayList("m1", "m2", "m3", "m4"), Lists.newArrayList(.2, .4, .5, .6)));
    logAnalysisService.saveAnalysis(learningEngineTask2.getUuid(), createAnalysisDTO(instant, .45));
    List<RiskSummaryPopoverDTO.AnalysisRisk> analysisRisks = analysisService.getTop3AnalysisRisks(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, instant.minus(Duration.ofMinutes(15)), instant);

    assertThat(analysisRisks).hasSize(3);
    assertThat(analysisRisks.get(0).getName()).isEqualTo("exception");
    assertThat(analysisRisks.get(0).getRisk()).isEqualTo(45);
    for (int i = 1; i < analysisRisks.size(); i++) {
      assertThat(analysisRisks.get(i).getName()).isEqualTo("txn - m" + (2 + i));
      assertThat(analysisRisks.get(i).getRisk()).isEqualTo((int) ((0.4 + i * .1) * 100));
    }
  }

  private LogAnalysisDTO createAnalysisDTO(Instant endTime, double score) {
    List<LogAnalysisCluster> clusters = buildAnalysisClusters(1234l, 23456l);
    LogAnalysisResult result =
        LogAnalysisResult.builder().logAnalysisResults(getAnalysisResults(12345l, 23456l)).build();
    return LogAnalysisDTO.builder()
        .score(score)
        .verificationTaskId(logVerificationTaskId)
        .logClusters(clusters)
        .logAnalysisResults(result.getLogAnalysisResults())
        .analysisMinute(endTime.getEpochSecond() / 60)
        .build();
  }
  private List<LogAnalysisResult.AnalysisResult> getAnalysisResults(long... labels) {
    List<LogAnalysisResult.AnalysisResult> results = new ArrayList<>();
    for (int i = 0; i < labels.length; i++) {
      LogAnalysisResult.AnalysisResult analysisResult =
          LogAnalysisResult.AnalysisResult.builder()
              .count(3)
              .label(labels[i])
              .tag(i % 2 == 0 ? LogAnalysisResult.LogAnalysisTag.KNOWN : LogAnalysisResult.LogAnalysisTag.UNKNOWN)
              .build();
      results.add(analysisResult);
    }
    return results;
  }
  private List<LogAnalysisCluster> buildAnalysisClusters(long... labels) {
    List<LogAnalysisCluster> clusters = new ArrayList<>();
    for (long label : labels) {
      LogAnalysisCluster cluster =
          LogAnalysisCluster.builder()
              .label(label)
              .isEvicted(false)
              .text("exception message")
              .frequencyTrend(
                  Arrays.asList(LogAnalysisCluster.Frequency.builder().count(1).timestamp(12353453L).build(),
                      LogAnalysisCluster.Frequency.builder().count(2).timestamp(12353453L).build(),
                      LogAnalysisCluster.Frequency.builder().count(3).timestamp(12353453L).build(),
                      LogAnalysisCluster.Frequency.builder().count(4).timestamp(12353453L).build()))
              .build();
      clusters.add(cluster);
    }
    return clusters;
  }
  private LearningEngineTask create(String verificationTaskId, LearningEngineTaskType analysisType) {
    ServiceGuardLogAnalysisTask task = ServiceGuardLogAnalysisTask.builder().build();
    task.setTestDataUrl("testData");
    task.setTaskStatus(LearningEngineTask.ExecutionStatus.QUEUED);
    task.setVerificationTaskId(verificationTaskId);
    task.setAnalysisType(analysisType);
    task.setFailureUrl("failure-url");
    task.setAnalysisStartTime(instant.minus(Duration.ofMinutes(10)));
    task.setAnalysisEndTime(instant);
    Instant start = instant.minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    task.setAnalysisStartTime(start);
    task.setAnalysisEndTime(end);

    learningEngineTaskService.createLearningEngineTask(task);
    return task;
  }

  private LearningEngineTask createTimeseriesTask(String verificationTaskId) {
    TimeSeriesLearningEngineTask task = TimeSeriesLearningEngineTask.builder().build();
    task.setTestDataUrl("testData");
    task.setTaskStatus(LearningEngineTask.ExecutionStatus.QUEUED);
    task.setVerificationTaskId(verificationTaskId);
    task.setAnalysisType(LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES);
    task.setFailureUrl("failure-url");
    task.setAnalysisStartTime(instant.minus(Duration.ofMinutes(10)));
    task.setAnalysisEndTime(instant);
    Instant start = instant.minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    task.setAnalysisStartTime(start);
    task.setAnalysisEndTime(end);
    task.setWindowSize(5);

    learningEngineTaskService.createLearningEngineTask(task);
    return task;
  }

  private ServiceGuardTimeSeriesAnalysisDTO buildServiceGuardMetricAnalysisDTO(
      List<String> metricNames, List<Double> scores) {
    Map<String, Double> overallMetricScores = new HashMap<>();
    overallMetricScores.put("Errors per Minute", 0.872);
    overallMetricScores.put("Average Response Time", 0.212);
    overallMetricScores.put("Calls Per Minute", 0.0);
    Map<String, Map<String, ServiceGuardTxnMetricAnalysisDataDTO>> txnMetricMap = new HashMap<>();
    String txn = "txn";
    txnMetricMap.put(txn, new HashMap<>());
    for (int i = 0; i < metricNames.size(); i++) {
      String metric = metricNames.get(i);
      Map<String, ServiceGuardTxnMetricAnalysisDataDTO> metricMap = txnMetricMap.get(txn);
      ServiceGuardTxnMetricAnalysisDataDTO txnMetricData =
          ServiceGuardTxnMetricAnalysisDataDTO.builder()
              .isKeyTransaction(false)
              .cumulativeSums(TimeSeriesCumulativeSums.MetricSum.builder().risk(0.5).data(0.9).build())
              .shortTermHistory(Arrays.asList(0.1, 0.2, 0.3, 0.4))
              .anomalousPatterns(Arrays.asList(TimeSeriesAnomalies.builder()
                                                   .transactionName(txn)
                                                   .metricName(metric)
                                                   .testData(Arrays.asList(0.1, 0.2, 0.3, 0.4))
                                                   .anomalousTimestamps(Arrays.asList(12345l, 12346l, 12347l))
                                                   .build()))
              .lastSeenTime(0)
              .metricType(TimeSeriesMetricType.ERROR)
              .risk(1)
              .score(scores.get(i))
              .build();
      metricMap.put(metric, txnMetricData);
    }

    return ServiceGuardTimeSeriesAnalysisDTO.builder()
        .verificationTaskId(metricVerificationTaskId)
        .analysisStartTime(Instant.now().minus(10, ChronoUnit.MINUTES))
        .analysisEndTime(Instant.now().minus(5, ChronoUnit.MINUTES))
        .overallMetricScores(overallMetricScores)
        .txnMetricAnalysisData(txnMetricMap)
        .build();
  }

  private CVConfig createSplunkCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier("host");
    return cvConfig;
  }

  private CVConfig creatAppDynamicsCVConfig() {
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    fillCommon(cvConfig);
    cvConfig.setApplicationName("cv-app");
    cvConfig.setTierName("docker-tier");
    cvConfig.setMetricPack(MetricPack.builder().build());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier(serviceIdentifier);
    cvConfig.setEnvIdentifier("env");
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setIdentifier(generateUuid());
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
  }
}
