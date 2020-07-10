package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.ServiceGuardTxnMetricAnalysisDataDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TimeSeriesAnalysisServiceImplTest extends CVNextGenBaseTest {
  @Mock LearningEngineTaskService learningEngineTaskService;
  @Mock TimeSeriesService timeSeriesService;
  @Inject TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject HPersistence hPersistence;

  private String cvConfigId;

  @Before
  public void setUp() throws Exception {
    cvConfigId = generateUuid();
    FieldUtils.writeField(timeSeriesAnalysisService, "timeSeriesService", timeSeriesService, true);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testScheduleAnalysis() {
    AnalysisInput input = AnalysisInput.builder()
                              .cvConfigId(cvConfigId)
                              .startTime(Instant.now().minus(10, ChronoUnit.MINUTES))
                              .endTime(Instant.now())
                              .build();
    List<String> taskIds = timeSeriesAnalysisService.scheduleAnalysis(cvConfigId, input);

    assertThat(taskIds).isNotNull();

    assertThat(taskIds.size()).isEqualTo(1);

    LearningEngineTask task = hPersistence.get(LearningEngineTask.class, taskIds.get(0));
    assertThat(task).isNotNull();
    assertThat(task.getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(Duration.between(task.getAnalysisStartTime(), input.getStartTime())).isZero();
    assertThat(Duration.between(task.getAnalysisEndTime(), input.getEndTime())).isZero();
    assertThat(task.getAnalysisType().name()).isEqualTo(LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetTaskStatus() throws Exception {
    FieldUtils.writeField(timeSeriesAnalysisService, "learningEngineTaskService", learningEngineTaskService, true);
    Set<String> taskIds = new HashSet<>();
    taskIds.add("task1");
    taskIds.add("task2");
    timeSeriesAnalysisService.getTaskStatus(cvConfigId, taskIds);

    Mockito.verify(learningEngineTaskService).getTaskStatus(taskIds);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetMetricTemplate() {
    timeSeriesAnalysisService.getMetricTemplate(cvConfigId);

    Mockito.verify(timeSeriesService).getTimeSeriesMetricDefinitions(cvConfigId);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetCumulativeSums() {
    Instant start = Instant.now().minus(10, ChronoUnit.MINUTES);
    Instant end = Instant.now().minus(5, ChronoUnit.MINUTES);
    TimeSeriesCumulativeSums cumulativeSums = TimeSeriesCumulativeSums.builder()
                                                  .cvConfigId(cvConfigId)
                                                  .analysisStartTime(start)
                                                  .analysisEndTime(end)
                                                  .transactionMetricSums(buildTransactionMetricSums())
                                                  .build();

    hPersistence.save(cumulativeSums);
    Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> actual =
        timeSeriesAnalysisService.getCumulativeSums(cvConfigId, start, end);
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
    List<TimeSeriesRecord> records = getTimeSeriesRecords();
    hPersistence.save(records);
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Map<String, Map<String, List<Double>>> testData =
        timeSeriesAnalysisService.getTestData(cvConfigId, start, start.plus(5, ChronoUnit.MINUTES));

    assertThat(testData).isNotNull();
    assertThat(testData.size()).isEqualTo(61);
    testData.forEach((txn, metricMap) -> { assertThat(metricMap.size()).isEqualTo(2); });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetLongTermAnomalies() {
    TimeSeriesAnomalousPatterns patterns = TimeSeriesAnomalousPatterns.builder()
                                               .cvConfigId(cvConfigId)
                                               .anomalies(buildAnomList())
                                               .uuid("patternsUuid")
                                               .build();
    hPersistence.save(patterns);

    Map<String, Map<String, List<TimeSeriesAnomalies>>> actual =
        timeSeriesAnalysisService.getLongTermAnomalies(cvConfigId);
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
                                                      .cvConfigId(cvConfigId)
                                                      .transactionMetricHistories(buildShortTermHistory())
                                                      .build();
    hPersistence.save(shortTermHistory);
    Map<String, Map<String, List<Double>>> actual = timeSeriesAnalysisService.getShortTermHistory(cvConfigId);
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
    Map<String, Map<String, List<Double>>> actual = timeSeriesAnalysisService.getShortTermHistory(cvConfigId);
    assertThat(actual).isNotNull();
    assertThat(actual.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testSaveAnalysis() {
    timeSeriesAnalysisService.saveAnalysis(buildServiceGuardMetricAnalysisDTO(), cvConfigId, "letTaskId",
        Instant.now().minus(10, ChronoUnit.MINUTES), Instant.now().minus(5, ChronoUnit.MINUTES));

    TimeSeriesCumulativeSums cumulativeSums =
        hPersistence.createQuery(TimeSeriesCumulativeSums.class).filter("cvConfigId", cvConfigId).get();
    assertThat(cumulativeSums).isNotNull();
    TimeSeriesAnomalousPatterns anomalousPatterns =
        hPersistence.createQuery(TimeSeriesAnomalousPatterns.class).filter("cvConfigId", cvConfigId).get();
    assertThat(anomalousPatterns).isNotNull();
    TimeSeriesShortTermHistory shortTermHistory =
        hPersistence.createQuery(TimeSeriesShortTermHistory.class).filter("cvConfigId", cvConfigId).get();
    assertThat(shortTermHistory).isNotNull();
  }

  private ServiceGuardMetricAnalysisDTO buildServiceGuardMetricAnalysisDTO() {
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

    return ServiceGuardMetricAnalysisDTO.builder()
        .cvConfigId(cvConfigId)
        .analysisStartTime(Instant.now().minus(10, ChronoUnit.MINUTES))
        .analysisEndTime(Instant.now().minus(5, ChronoUnit.MINUTES))
        .overallMetricScores(overallMetricScores)
        .txnMetricAnalysisData(txnMetricMap)
        .build();
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
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
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
}