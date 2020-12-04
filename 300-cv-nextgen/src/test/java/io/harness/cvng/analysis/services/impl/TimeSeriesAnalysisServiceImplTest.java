package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SERVICE_GUARD_DATA_LENGTH;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SERVICE_GUARD_WINDOW_SIZE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.CVConstants;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardTxnMetricAnalysisDataDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.beans.TimeSeriesRecordDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesCanaryLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesLoadTestLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary.TransactionMetricRisk;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.verificationjob.beans.CanaryVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class TimeSeriesAnalysisServiceImplTest extends CvNextGenTest {
  @Inject LearningEngineTaskService learningEngineTaskService;
  @Mock TimeSeriesService mockTimeSeriesService;
  @Mock AnomalyService anomalyService;
  @Mock CVConfigService cvConfigService;
  @Inject TimeSeriesService timeSeriesService;
  @Inject TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject HPersistence hPersistence;
  @Inject VerificationTaskService verificationTaskService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationJobService verificationJobService;

  private String cvConfigId;
  private String verificationTaskId;
  private String learningEngineTaskId;
  private String accountId;
  private long deploymentStartTimeMs;
  private Instant instant;
  @Before
  public void setUp() throws Exception {
    cvConfigId = generateUuid();
    accountId = generateUuid();
    instant = Instant.parse("2020-07-27T10:44:06.390Z");
    deploymentStartTimeMs = instant.toEpochMilli();
    FieldUtils.writeField(timeSeriesAnalysisService, "timeSeriesService", mockTimeSeriesService, true);
    FieldUtils.writeField(timeSeriesAnalysisService, "anomalyService", anomalyService, true);
    FieldUtils.writeField(timeSeriesAnalysisService, "cvConfigService", cvConfigService, true);

    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setAccountId(generateUuid());
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setUuid(cvConfigId);
    verificationTaskId = verificationTaskService.create(cvConfig.getAccountId(), cvConfigId);
    when(cvConfigService.get(cvConfigId)).thenReturn(cvConfig);
    TimeSeriesLearningEngineTask timeSeriesLearningEngineTask = TimeSeriesLearningEngineTask.builder().build();
    timeSeriesLearningEngineTask.setVerificationTaskId(verificationTaskId);
    timeSeriesLearningEngineTask.setAnalysisStartTime(Instant.now());
    timeSeriesLearningEngineTask.setAnalysisEndTime(Instant.now().plus(Duration.ofMinutes(5)));
    timeSeriesLearningEngineTask.setWindowSize(5);
    learningEngineTaskId = learningEngineTaskService.createLearningEngineTask(timeSeriesLearningEngineTask);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testScheduleServiceGuardAnalysis() {
    AnalysisInput input = AnalysisInput.builder()
                              .verificationTaskId(verificationTaskId)
                              .startTime(Instant.now().minus(10, ChronoUnit.MINUTES))
                              .endTime(Instant.now())
                              .build();
    List<String> taskIds = timeSeriesAnalysisService.scheduleServiceGuardAnalysis(input);

    assertThat(taskIds).isNotNull();

    assertThat(taskIds.size()).isEqualTo(1);

    TimeSeriesLearningEngineTask task =
        (TimeSeriesLearningEngineTask) hPersistence.get(LearningEngineTask.class, taskIds.get(0));
    assertThat(task).isNotNull();
    assertThat(task.getVerificationTaskId()).isEqualTo(verificationTaskId);
    assertThat(Duration.between(task.getAnalysisStartTime(), input.getStartTime())).isZero();
    assertThat(Duration.between(task.getAnalysisEndTime(), input.getEndTime())).isZero();
    assertThat(task.getAnalysisType().name()).isEqualTo(LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES.name());
    assertThat(task.getDataLength()).isEqualTo(TIMESERIES_SERVICE_GUARD_DATA_LENGTH);
    assertThat(task.getWindowSize()).isEqualTo(TIMESERIES_SERVICE_GUARD_WINDOW_SIZE);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCumulativeSums() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES);
    Instant end = Instant.now().minus(5, ChronoUnit.MINUTES);
    TimeSeriesCumulativeSums cumulativeSums = TimeSeriesCumulativeSums.builder()
                                                  .verificationTaskId(verificationTaskId)
                                                  .analysisStartTime(start)
                                                  .analysisEndTime(end)
                                                  .transactionMetricSums(buildTransactionMetricSums())
                                                  .build();

    hPersistence.save(cumulativeSums);
    Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> actual =
        timeSeriesAnalysisService.getCumulativeSums(verificationTaskId, start, end);
    Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> expected = cumulativeSums.convertToMap();
    expected.forEach((key, map) -> {
      assertThat(actual.containsKey(key));
      map.forEach((metric, cumsum) -> {
        assertThat(actual.get(key).containsKey(metric));
        assertThat(cumsum).isEqualTo(actual.get(key).get(metric));
      });
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetTestData() throws Exception {
    FieldUtils.writeField(timeSeriesAnalysisService, "timeSeriesService", timeSeriesService, true);
    List<TimeSeriesRecord> records = getTimeSeriesRecords();
    hPersistence.save(records);
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    List<TimeSeriesRecordDTO> testData =
        timeSeriesAnalysisService.getTimeSeriesRecordDTOs(cvConfigId, start, start.plus(5, ChronoUnit.MINUTES));

    assertThat(testData).isNotNull();
    assertThat(testData.size()).isEqualTo(183);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetLongTermAnomalies() {
    TimeSeriesAnomalousPatterns patterns = TimeSeriesAnomalousPatterns.builder()
                                               .verificationTaskId(verificationTaskId)
                                               .anomalies(buildAnomList())
                                               .uuid("patternsUuid")
                                               .build();
    hPersistence.save(patterns);

    Map<String, Map<String, List<TimeSeriesAnomalies>>> actual =
        timeSeriesAnalysisService.getLongTermAnomalies(verificationTaskId);
    Map<String, Map<String, List<TimeSeriesAnomalies>>> expected = patterns.convertToMap();
    expected.forEach((key, map) -> {
      assertThat(actual.containsKey(key));
      map.forEach((metric, anomList) -> {
        assertThat(actual.get(key).containsKey(metric));
        assertThat(anomList).isEqualTo(actual.get(key).get(metric));
      });
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetLongTermAnomalies_noPreviousAnoms() {
    Map<String, Map<String, List<TimeSeriesAnomalies>>> actual =
        timeSeriesAnalysisService.getLongTermAnomalies(cvConfigId);
    assertThat(actual).isNotNull();
    assertThat(actual.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetShortTermHistory() {
    TimeSeriesShortTermHistory shortTermHistory = TimeSeriesShortTermHistory.builder()
                                                      .verificationTaskId(verificationTaskId)
                                                      .transactionMetricHistories(buildShortTermHistory())
                                                      .build();
    hPersistence.save(shortTermHistory);
    Map<String, Map<String, List<Double>>> actual = timeSeriesAnalysisService.getShortTermHistory(verificationTaskId);
    Map<String, Map<String, List<Double>>> expected = shortTermHistory.convertToMap();

    expected.forEach((key, map) -> {
      assertThat(actual.containsKey(key));
      map.forEach((metric, history) -> {
        assertThat(actual.get(key).containsKey(metric));
        assertThat(history).isEqualTo(actual.get(key).get(metric));
      });
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetShortTermHistory_noPreviousHistory() {
    Map<String, Map<String, List<Double>>> actual = timeSeriesAnalysisService.getShortTermHistory(verificationTaskId);
    assertThat(actual).isNotNull();
    assertThat(actual.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testSaveAnalysis_serviceGuard() {
    when(cvConfigService.get(cvConfigId)).thenReturn(null);
    timeSeriesAnalysisService.saveAnalysis(learningEngineTaskId, buildServiceGuardMetricAnalysisDTO());

    TimeSeriesCumulativeSums cumulativeSums =
        hPersistence.createQuery(TimeSeriesCumulativeSums.class).filter("verificationTaskId", verificationTaskId).get();
    assertThat(cumulativeSums).isNotNull();
    TimeSeriesAnomalousPatterns anomalousPatterns = hPersistence.createQuery(TimeSeriesAnomalousPatterns.class)
                                                        .filter("verificationTaskId", verificationTaskId)
                                                        .get();
    assertThat(anomalousPatterns).isNotNull();
    TimeSeriesShortTermHistory shortTermHistory = hPersistence.createQuery(TimeSeriesShortTermHistory.class)
                                                      .filter("verificationTaskId", verificationTaskId)
                                                      .get();
    assertThat(shortTermHistory).isNotNull();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSaveAnalysis_serviceGuard_withoutCumulativeSums() {
    when(cvConfigService.get(cvConfigId)).thenReturn(null);
    timeSeriesAnalysisService.saveAnalysis(
        learningEngineTaskId, buildServiceGuardMetricAnalysisDTO_emptyCumulativeSums());

    TimeSeriesCumulativeSums cumulativeSums =
        hPersistence.createQuery(TimeSeriesCumulativeSums.class).filter("verificationTaskId", verificationTaskId).get();
    assertThat(cumulativeSums).isNotNull();
    assertThat(cumulativeSums.convertToMap()).isEmpty();
    TimeSeriesAnomalousPatterns anomalousPatterns = hPersistence.createQuery(TimeSeriesAnomalousPatterns.class)
                                                        .filter("verificationTaskId", verificationTaskId)
                                                        .get();
    assertThat(anomalousPatterns).isNotNull();
    TimeSeriesShortTermHistory shortTermHistory = hPersistence.createQuery(TimeSeriesShortTermHistory.class)
                                                      .filter("verificationTaskId", verificationTaskId)
                                                      .get();
    assertThat(shortTermHistory).isNotNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSaveAnalysis_deploymentVerification() {
    timeSeriesAnalysisService.saveAnalysis(learningEngineTaskId, buildDeploymentVerificationDTO());
    List<DeploymentTimeSeriesAnalysis> results =
        deploymentTimeSeriesAnalysisService.getAnalysisResults(verificationTaskId);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getVerificationTaskId()).isEqualTo(verificationTaskId);
  }

  private ServiceGuardTimeSeriesAnalysisDTO buildServiceGuardMetricAnalysisDTO() {
    Map<String, Double> overallMetricScores = new HashMap<>();
    overallMetricScores.put("Errors per Minute", 0.872);
    overallMetricScores.put("Average Response Time", 0.212);
    overallMetricScores.put("Calls Per Minute", 0.0);

    List<String> transactions = Arrays.asList("txn1", "txn2", "txn3");
    List<String> metricList = Arrays.asList("metric1", "metric2", "metric3");
    Map<String, Map<String, ServiceGuardTxnMetricAnalysisDataDTO>> txnMetricMap = new HashMap<>();
    transactions.forEach(txn -> {
      txnMetricMap.put(txn, new HashMap<>());
      metricList.forEach(metric -> {
        Map<String, ServiceGuardTxnMetricAnalysisDataDTO> metricMap = txnMetricMap.get(txn);
        ServiceGuardTxnMetricAnalysisDataDTO txnMetricData =
            ServiceGuardTxnMetricAnalysisDataDTO.builder()
                .isKeyTransaction(false)
                .cumulativeSums(TimeSeriesCumulativeSums.MetricSum.builder().risk(0.5).sum(0.9).build())
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
                .build();
        metricMap.put(metric, txnMetricData);
      });
    });

    return ServiceGuardTimeSeriesAnalysisDTO.builder()
        .verificationTaskId(verificationTaskId)
        .analysisStartTime(Instant.now().minus(10, ChronoUnit.MINUTES))
        .analysisEndTime(Instant.now().minus(5, ChronoUnit.MINUTES))
        .overallMetricScores(overallMetricScores)
        .txnMetricAnalysisData(txnMetricMap)
        .build();
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
              .cumulativeSums(TimeSeriesCumulativeSums.MetricSum.builder().risk(0.5).sum(0.9).build())
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
        .verificationTaskId(verificationTaskId)
        .analysisStartTime(Instant.now().minus(10, ChronoUnit.MINUTES))
        .analysisEndTime(Instant.now().minus(5, ChronoUnit.MINUTES))
        .overallMetricScores(overallMetricScores)
        .txnMetricAnalysisData(txnMetricMap)
        .build();
  }

  private ServiceGuardTimeSeriesAnalysisDTO buildServiceGuardMetricAnalysisDTO_emptyCumulativeSums() {
    Map<String, Double> overallMetricScores = new HashMap<>();
    overallMetricScores.put("Errors per Minute", 0.872);
    overallMetricScores.put("Average Response Time", 0.212);
    overallMetricScores.put("Calls Per Minute", 0.0);

    List<String> transactions = Arrays.asList("txn1", "txn2", "txn3");
    List<String> metricList = Arrays.asList("metric1", "metric2", "metric3");
    Map<String, Map<String, ServiceGuardTxnMetricAnalysisDataDTO>> txnMetricMap = new HashMap<>();
    transactions.forEach(txn -> {
      txnMetricMap.put(txn, new HashMap<>());
      metricList.forEach(metric -> {
        Map<String, ServiceGuardTxnMetricAnalysisDataDTO> metricMap = txnMetricMap.get(txn);
        ServiceGuardTxnMetricAnalysisDataDTO txnMetricData =
            ServiceGuardTxnMetricAnalysisDataDTO.builder()
                .isKeyTransaction(false)
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
                .build();
        metricMap.put(metric, txnMetricData);
      });
    });

    return ServiceGuardTimeSeriesAnalysisDTO.builder()
        .verificationTaskId(verificationTaskId)
        .analysisStartTime(Instant.now().minus(10, ChronoUnit.MINUTES))
        .analysisEndTime(Instant.now().minus(5, ChronoUnit.MINUTES))
        .overallMetricScores(overallMetricScores)
        .txnMetricAnalysisData(txnMetricMap)
        .build();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testScheduleCanaryVerificationTaskAnalysis() {
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance(VerificationJobType.CANARY);
    cvConfigService.save(newCVConfig());
    AnalysisInput input = AnalysisInput.builder()
                              .verificationTaskId(verificationTaskId)
                              .startTime(instant.plus(10, ChronoUnit.MINUTES))
                              .endTime(instant.plus(11, ChronoUnit.MINUTES))
                              .build();

    List<String> taskIds = timeSeriesAnalysisService.scheduleCanaryVerificationTaskAnalysis(input);
    assertThat(taskIds).isNotNull();

    assertThat(taskIds.size()).isEqualTo(1);

    TimeSeriesCanaryLearningEngineTask task =
        (TimeSeriesCanaryLearningEngineTask) hPersistence.get(LearningEngineTask.class, taskIds.get(0));
    assertThat(task).isNotNull();
    assertThat(task.getVerificationTaskId()).isEqualTo(verificationTaskId);
    assertThat(task.getDataLength()).isEqualTo(9);
    assertThat(task.getDeploymentVerificationTaskInfo().getDeploymentStartTime())
        .isEqualTo(verificationJobInstance.getDeploymentStartTime().toEpochMilli());
    assertThat(Duration.between(task.getAnalysisStartTime(), input.getStartTime())).isZero();
    assertThat(Duration.between(task.getAnalysisEndTime(), input.getEndTime())).isZero();
    assertThat(task.getAnalysisType().name()).isEqualTo(LearningEngineTaskType.TIME_SERIES_CANARY.name());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testScheduleTestVerificationTaskAnalysis() {
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance(VerificationJobType.TEST);
    String verificationTaskId =
        verificationTaskService.getVerificationTaskId(cvConfigId, verificationJobInstance.getUuid());
    cvConfigService.save(newCVConfig());
    AnalysisInput input = AnalysisInput.builder()
                              .verificationTaskId(verificationTaskId)
                              .startTime(instant.plus(10, ChronoUnit.MINUTES))
                              .endTime(instant.plus(11, ChronoUnit.MINUTES))
                              .build();
    List<String> taskIds = timeSeriesAnalysisService.scheduleTestVerificationTaskAnalysis(input);
    assertThat(taskIds.size()).isEqualTo(1);
    TimeSeriesLoadTestLearningEngineTask task =
        (TimeSeriesLoadTestLearningEngineTask) hPersistence.get(LearningEngineTask.class, taskIds.get(0));
    assertThat(task).isNotNull();
    assertThat(task.getVerificationTaskId()).isEqualTo(verificationTaskId);
    assertThat(Duration.between(task.getAnalysisStartTime(), input.getStartTime())).isZero();
    assertThat(Duration.between(task.getAnalysisEndTime(), input.getEndTime())).isZero();
    assertThat(task.getAnalysisType().name()).isEqualTo(LearningEngineTaskType.TIME_SERIES_LOAD_TEST.name());
    assertThat(task.getControlDataUrl())
        .isEqualTo("/cv/api/timeseries-analysis/time-series-data?verificationTaskId=" + verificationTaskId
            + "&startTime=1595846766390&endTime=1595847666390");
    assertThat(task.getBaselineStartTime()).isEqualTo(1595846766390L);
    assertThat(task.getTestDataUrl())
        .isEqualTo("/cv/api/timeseries-analysis/time-series-data?verificationTaskId=" + verificationTaskId
            + "&startTime=1595846766390&endTime=1595847306390");
    assertThat(task.getMetricTemplateUrl())
        .isEqualTo("/cv/api/timeseries-analysis/timeseries-serviceguard-metric-template?verificationTaskId="
            + verificationTaskId);
    assertThat(task.getDataLength()).isEqualTo(9);
    assertThat(task.getTolerance()).isEqualTo(2);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testScheduleTestVerificationTaskAnalysis_baselineRun() {
    VerificationJobInstance verificationJobInstance = createVerificationJobInstance(VerificationJobType.TEST);
    ((TestVerificationJob) verificationJobInstance.getResolvedJob()).setBaselineVerificationJobInstanceId(null);
    verificationJobInstanceService.create(verificationJobInstance);
    String verificationTaskId =
        verificationTaskService.getVerificationTaskId(cvConfigId, verificationJobInstance.getUuid());
    cvConfigService.save(newCVConfig());
    AnalysisInput input = AnalysisInput.builder()
                              .verificationTaskId(verificationTaskId)
                              .startTime(instant.plus(10, ChronoUnit.MINUTES))
                              .endTime(instant.plus(11, ChronoUnit.MINUTES))
                              .build();
    List<String> taskIds = timeSeriesAnalysisService.scheduleTestVerificationTaskAnalysis(input);
    assertThat(taskIds.size()).isEqualTo(1);
    TimeSeriesLoadTestLearningEngineTask task =
        (TimeSeriesLoadTestLearningEngineTask) hPersistence.get(LearningEngineTask.class, taskIds.get(0));
    assertThat(task).isNotNull();
    assertThat(task.getVerificationTaskId()).isEqualTo(verificationTaskId);
    assertThat(Duration.between(task.getAnalysisStartTime(), input.getStartTime())).isZero();
    assertThat(Duration.between(task.getAnalysisEndTime(), input.getEndTime())).isZero();
    assertThat(task.getAnalysisType().name()).isEqualTo(LearningEngineTaskType.TIME_SERIES_LOAD_TEST.name());
    assertThat(task.getControlDataUrl()).isNull();
    assertThat(task.getBaselineStartTime()).isNull();
    assertThat(task.getTestDataUrl())
        .isEqualTo(CVConstants.SERVICE_BASE_URL + "/timeseries-analysis/time-series-data?verificationTaskId="
            + verificationTaskId + "&startTime=1595846766390&endTime=1595847306390");
    assertThat(task.getMetricTemplateUrl())
        .isEqualTo(CVConstants.SERVICE_BASE_URL
            + "/timeseries-analysis/timeseries-serviceguard-metric-template?verificationTaskId=" + verificationTaskId);
    assertThat(task.getDataLength()).isEqualTo(9);
    assertThat(task.getTolerance()).isEqualTo(2);
  }

  private DeploymentTimeSeriesAnalysisDTO buildDeploymentVerificationDTO() {
    return DeploymentTimeSeriesAnalysisDTO.builder().build();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTopTimeSeriesTransactionMetricRisk_validOrder() throws IllegalAccessException {
    FieldUtils.writeField(timeSeriesAnalysisService, "heatMapService", mock(HeatMapService.class), true);
    TimeSeriesLearningEngineTask task = TimeSeriesLearningEngineTask.builder().build();
    task.setTestDataUrl("testData");
    task.setWindowSize(5);
    fillCommon(task, LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES);
    Instant start = instant.minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    task.setAnalysisStartTime(start);
    task.setAnalysisEndTime(end);

    learningEngineTaskService.createLearningEngineTask(task);
    String metricNames[] = {"metric1", "metric2", "metric3", "metrics4"};
    Double scores[] = {.1, .2, .3, .4};
    for (int i = 0; i < 2; i++) {
      timeSeriesAnalysisService.saveAnalysis(
          task.getUuid(), buildServiceGuardMetricAnalysisDTO(Arrays.asList(metricNames), Arrays.asList(scores)));
    }
    List<TransactionMetricRisk> transactionMetricRisks =
        timeSeriesAnalysisService.getTopTimeSeriesTransactionMetricRisk(Collections.singletonList(verificationTaskId),
            end.minus(Duration.ofMinutes(10)), end.plus(Duration.ofMinutes(1)));
    assertThat(transactionMetricRisks).hasSize(3);

    for (int i = 0; i < 3; i++) {
      assertThat(transactionMetricRisks.get(i).getMetricName()).isEqualTo(metricNames[i + 1]);
      assertThat(transactionMetricRisks.get(i).getTransactionName()).isEqualTo("txn");
      assertThat(transactionMetricRisks.get(i).getMetricScore()).isEqualTo(scores[i + 1], offset(.0001));
    }
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTopTimeSeriesTransactionMetricRisk_empty() {
    assertThat(
        timeSeriesAnalysisService.getTopTimeSeriesTransactionMetricRisk(Collections.singletonList(generateUuid()),
            instant.minus(Duration.ofMinutes(10)), instant.plus(Duration.ofMinutes(1))))
        .isEmpty();
  }

  private List<TimeSeriesAnomalies> buildAnomList() {
    List<String> transactions = Arrays.asList("txn1", "txn2", "txn3");
    List<String> metricList = Arrays.asList("metric1", "metric2", "metric3");
    List<TimeSeriesAnomalies> anomList = new ArrayList<>();
    transactions.forEach(txn -> {
      metricList.forEach(metric -> {
        TimeSeriesAnomalies anomalies = TimeSeriesAnomalies.builder()
                                            .transactionName(txn)
                                            .metricName(metric)
                                            .testData(Arrays.asList(0.1, 0.2, 0.3, 0.4))
                                            .anomalousTimestamps(Arrays.asList(12345l, 12346l, 12347l))
                                            .build();
        anomList.add(anomalies);
      });
    });
    return anomList;
  }

  private List<TimeSeriesShortTermHistory.TransactionMetricHistory> buildShortTermHistory() {
    List<String> transactions = Arrays.asList("txn1", "txn2", "txn3");
    List<String> metricList = Arrays.asList("metric1", "metric2", "metric3");

    List<TimeSeriesShortTermHistory.TransactionMetricHistory> shortTermHistoryList = new ArrayList<>();

    transactions.forEach(txn -> {
      TimeSeriesShortTermHistory.TransactionMetricHistory transactionMetricHistory =
          TimeSeriesShortTermHistory.TransactionMetricHistory.builder()
              .transactionName(txn)
              .metricHistoryList(new ArrayList<>())
              .build();
      metricList.forEach(metric -> {
        TimeSeriesShortTermHistory.MetricHistory metricHistory = TimeSeriesShortTermHistory.MetricHistory.builder()
                                                                     .metricName(metric)
                                                                     .value(Arrays.asList(0.1, 0.2, 0.3, 0.4))
                                                                     .build();
        transactionMetricHistory.getMetricHistoryList().add(metricHistory);
      });
      shortTermHistoryList.add(transactionMetricHistory);
    });
    return shortTermHistoryList;
  }

  private List<TimeSeriesCumulativeSums.TransactionMetricSums> buildTransactionMetricSums() {
    List<TimeSeriesCumulativeSums.TransactionMetricSums> txnMetricSums = new ArrayList<>();
    List<String> transactions = Arrays.asList("txn1", "txn2", "txn3");
    List<String> metricList = Arrays.asList("metric1", "metric2", "metric3");

    transactions.forEach(txn -> {
      TimeSeriesCumulativeSums.TransactionMetricSums transactionMetricSums =
          TimeSeriesCumulativeSums.TransactionMetricSums.builder()
              .transactionName(txn)
              .metricSums(new ArrayList<>())
              .build();

      metricList.forEach(metric -> {
        TimeSeriesCumulativeSums.MetricSum metricSums =
            TimeSeriesCumulativeSums.MetricSum.builder().metricName(metric).risk(0.5).sum(0.9).build();
        transactionMetricSums.getMetricSums().add(metricSums);
      });
      txnMetricSums.add(transactionMetricSums);
    });
    return txnMetricSums;
  }

  private List<TimeSeriesRecord> getTimeSeriesRecords() throws Exception {
    File file = new File(getClass().getClassLoader().getResource("timeseries/timeseriesRecords.json").getFile());
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesRecord>>() {}.getType();
      List<TimeSeriesRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setVerificationTaskId(verificationTaskId);
        timeSeriesMLAnalysisRecord.setBucketStartTime(Instant.parse("2020-07-07T02:40:00.000Z"));
        timeSeriesMLAnalysisRecord.getTimeSeriesGroupValues().forEach(groupVal -> {
          Instant baseTime = Instant.parse("2020-07-07T02:40:00.000Z");
          Random random = new Random();
          groupVal.setTimeStamp(baseTime.plus(random.nextInt(4), ChronoUnit.MINUTES));
        });
      });
      return timeSeriesMLAnalysisRecords;
    }
  }

  private VerificationJobDTO newVerificationJobDTO(VerificationJobType type) {
    if (type == VerificationJobType.TEST) {
      TestVerificationJobDTO testVerificationJob = new TestVerificationJobDTO();
      testVerificationJob.setIdentifier(generateUuid());
      testVerificationJob.setJobName(generateUuid());
      testVerificationJob.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
      testVerificationJob.setServiceIdentifier(generateUuid());
      testVerificationJob.setOrgIdentifier(generateUuid());
      testVerificationJob.setProjectIdentifier(generateUuid());
      testVerificationJob.setEnvIdentifier(generateUuid());
      testVerificationJob.setSensitivity(Sensitivity.MEDIUM.name());
      testVerificationJob.setDuration("15m");
      testVerificationJob.setBaselineVerificationJobInstanceId(generateUuid());
      return testVerificationJob;
    } else {
      CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
      canaryVerificationJobDTO.setIdentifier(generateUuid());
      canaryVerificationJobDTO.setJobName(generateUuid());
      canaryVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
      canaryVerificationJobDTO.setServiceIdentifier(generateUuid());
      canaryVerificationJobDTO.setOrgIdentifier(generateUuid());
      canaryVerificationJobDTO.setProjectIdentifier(generateUuid());
      canaryVerificationJobDTO.setEnvIdentifier(generateUuid());
      canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
      canaryVerificationJobDTO.setDuration("15m");
      return canaryVerificationJobDTO;
    }
  }

  private CVConfig newCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setUuid(cvConfigId);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier("serviceInstanceIdentifier");
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(generateUuid());
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId("groupId");
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName("productName");
    return cvConfig;
  }
  private VerificationJobInstance createVerificationJobInstance(VerificationJobType type) {
    VerificationJobDTO verificationJobDTO = newVerificationJobDTO(type);
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJob verificationJob =
        verificationJobService.getVerificationJob(accountId, verificationJobDTO.getIdentifier());
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder()
            .accountId(accountId)
            .executionStatus(VerificationJobInstance.ExecutionStatus.QUEUED)
            .verificationJobIdentifier(verificationJob.getIdentifier())
            .deploymentStartTime(Instant.ofEpochMilli(deploymentStartTimeMs))
            .resolvedJob(verificationJob)
            .startTime(Instant.ofEpochMilli(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis()))
            .build();
    if (type == VerificationJobType.TEST) {
      verificationJobInstance.setUuid(((TestVerificationJob) verificationJob).getBaselineVerificationJobInstanceId());
    }
    verificationJobInstanceService.create(verificationJobInstance);
    verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstance.getUuid());
    return verificationJobInstance;
  }
  private void fillCommon(LearningEngineTask learningEngineTask, LearningEngineTaskType analysisType) {
    learningEngineTask.setTaskStatus(LearningEngineTask.ExecutionStatus.QUEUED);
    learningEngineTask.setVerificationTaskId(verificationTaskId);
    learningEngineTask.setAnalysisType(analysisType);
    learningEngineTask.setFailureUrl("failure-url");
    learningEngineTask.setAnalysisStartTime(instant.minus(Duration.ofMinutes(10)));
    learningEngineTask.setAnalysisEndTime(instant);
  }
}
