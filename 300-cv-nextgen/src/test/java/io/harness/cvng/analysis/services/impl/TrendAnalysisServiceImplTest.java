package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.analysis.CVAnalysisConstants.TREND_METRIC_NAME;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardTxnMetricAnalysisDataDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisCluster.Frequency;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.TrendAnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TrendAnalysisServiceImplTest extends CvNextGenTest {
  private String cvConfigId;
  private String verificationTaskId;
  @Inject private HPersistence hPersistence;
  @Inject private LearningEngineTaskService learningEngineTaskService;
  @Inject private TrendAnalysisService trendAnalysisService;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    CVConfig cvConfig = createCVConfig();
    cvConfigService.save(cvConfig);
    cvConfigId = cvConfig.getUuid();
    verificationTaskId = verificationTaskService.getServiceGuardVerificationTaskId(cvConfig.getAccountId(), cvConfigId);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testScheduleTrendAnalysisTask() {
    AnalysisInput input = AnalysisInput.builder()
                              .verificationTaskId(verificationTaskId)
                              .startTime(Instant.now().minus(10, ChronoUnit.MINUTES))
                              .endTime(Instant.now())
                              .build();
    String taskId = trendAnalysisService.scheduleTrendAnalysisTask(input);

    assertThat(taskId).isNotNull();

    LearningEngineTask task = hPersistence.get(LearningEngineTask.class, taskId);
    assertThat(task).isNotNull();
    assertThat(task.getVerificationTaskId()).isEqualTo(verificationTaskId);
    assertThat(Duration.between(task.getAnalysisStartTime(), input.getStartTime())).isZero();
    assertThat(Duration.between(task.getAnalysisEndTime(), input.getEndTime())).isZero();
    assertThat(task.getAnalysisType().name()).isEqualTo(LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES.name());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetTestData() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<LogAnalysisCluster> logAnalysisClusters = createLogAnalysisClusters(start, end);
    hPersistence.save(logAnalysisClusters);

    Map<String, Map<String, List<Double>>> testData = trendAnalysisService.getTestData(verificationTaskId, start, end);

    assertThat(testData).isNotNull();
    assertThat(testData.size()).isEqualTo(logAnalysisClusters.size());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSaveAnalysis() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    List<LogAnalysisCluster> logAnalysisClusters = createLogAnalysisClusters(start, end);
    hPersistence.save(logAnalysisClusters);
    String learningEngineTaskId = saveLETask(start, end);

    trendAnalysisService.saveAnalysis(learningEngineTaskId, buildServiceGuardMetricAnalysisDTO(start, end));

    TimeSeriesCumulativeSums cumulativeSums =
        hPersistence.createQuery(TimeSeriesCumulativeSums.class).filter("cvConfigId", cvConfigId).get();
    assertThat(cumulativeSums).isNotNull();
    TimeSeriesAnomalousPatterns anomalousPatterns =
        hPersistence.createQuery(TimeSeriesAnomalousPatterns.class).filter("cvConfigId", cvConfigId).get();
    assertThat(anomalousPatterns).isNotNull();
    TimeSeriesShortTermHistory shortTermHistory =
        hPersistence.createQuery(TimeSeriesShortTermHistory.class).filter("cvConfigId", cvConfigId).get();
    assertThat(shortTermHistory).isNotNull();

    List<LogAnalysisCluster> savedClusters =
        hPersistence.createQuery(LogAnalysisCluster.class, excludeAuthority).asList();
    assertThat(savedClusters).isNotEmpty();
    assertThat(savedClusters.size()).isEqualTo(2);
    for (LogAnalysisCluster cluster : savedClusters) {
      for (Frequency frequency : cluster.getFrequencyTrend()) {
        assertThat(frequency.getRiskScore()).isEqualTo(1.0);
      }
    }
  }

  private String saveLETask(Instant start, Instant end) {
    TimeSeriesLearningEngineTask timeSeriesLearningEngineTask = TimeSeriesLearningEngineTask.builder().build();
    timeSeriesLearningEngineTask.setVerificationTaskId(verificationTaskId);
    timeSeriesLearningEngineTask.setAnalysisStartTime(start);
    timeSeriesLearningEngineTask.setAnalysisEndTime(end);
    return learningEngineTaskService.createLearningEngineTask(timeSeriesLearningEngineTask);
  }

  private ServiceGuardMetricAnalysisDTO buildServiceGuardMetricAnalysisDTO(Instant start, Instant end) {
    Map<String, Double> overallMetricScores = new HashMap<>();
    overallMetricScores.put(TREND_METRIC_NAME, 0.872);

    List<String> transactions = Arrays.asList("1", "2");
    Map<String, Map<String, ServiceGuardTxnMetricAnalysisDataDTO>> txnMetricMap = new HashMap<>();
    transactions.forEach(txn -> {
      txnMetricMap.put(txn, new HashMap<>());
      Map<String, ServiceGuardTxnMetricAnalysisDataDTO> metricMap = txnMetricMap.get(txn);
      ServiceGuardTxnMetricAnalysisDataDTO txnMetricData =
          ServiceGuardTxnMetricAnalysisDataDTO.builder()
              .isKeyTransaction(false)
              .cumulativeSums(TimeSeriesCumulativeSums.MetricSum.builder().risk(0.5).sum(0.9).build())
              .shortTermHistory(Arrays.asList(0.1, 0.2, 0.3, 0.4))
              .anomalousPatterns(
                  Collections.singletonList(TimeSeriesAnomalies.builder()
                                                .transactionName(txn)
                                                .metricName(TREND_METRIC_NAME)
                                                .testData(Arrays.asList(0.1, 0.2, 0.3, 0.4))
                                                .anomalousTimestamps(Arrays.asList(12345l, 12346l, 12347l))
                                                .build()))
              .lastSeenTime(0)
              .metricType(TimeSeriesMetricType.ERROR)
              .risk(1)
              .score(1.0)
              .build();
      metricMap.put(TREND_METRIC_NAME, txnMetricData);
    });

    return ServiceGuardMetricAnalysisDTO.builder()
        .cvConfigId(cvConfigId)
        .analysisStartTime(start)
        .analysisEndTime(end)
        .overallMetricScores(overallMetricScores)
        .txnMetricAnalysisData(txnMetricMap)
        .build();
  }

  private List<LogAnalysisCluster> createLogAnalysisClusters(Instant startTime, Instant endTime) {
    List<LogAnalysisCluster> logRecords = new ArrayList<>();

    LogAnalysisCluster record1 = LogAnalysisCluster.builder()
                                     .cvConfigId(verificationTaskId)
                                     .verificationTaskId(verificationTaskId)
                                     .label(1)
                                     .frequencyTrend(new ArrayList<>())
                                     .isEvicted(false)
                                     .build();
    LogAnalysisCluster record2 = LogAnalysisCluster.builder()
                                     .cvConfigId(verificationTaskId)
                                     .verificationTaskId(verificationTaskId)
                                     .label(2)
                                     .frequencyTrend(new ArrayList<>())
                                     .isEvicted(false)
                                     .build();
    Instant timestamp = startTime;
    while (timestamp.isBefore(endTime)) {
      Frequency frequency1 = Frequency.builder()
                                 .timestamp(TimeUnit.SECONDS.toMinutes(timestamp.getEpochSecond()))
                                 .count(4)
                                 .riskScore(0.5)
                                 .build();
      Frequency frequency2 = Frequency.builder()
                                 .timestamp(TimeUnit.SECONDS.toMinutes(timestamp.getEpochSecond()))
                                 .count(10)
                                 .riskScore(0.1)
                                 .build();
      record1.getFrequencyTrend().add(frequency1);
      record2.getFrequencyTrend().add(frequency2);
      timestamp = timestamp.plus(1, ChronoUnit.MINUTES);
    }
    logRecords.add(record1);
    logRecords.add(record2);

    return logRecords;
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(generateUuid());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(generateUuid());
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
  }
}