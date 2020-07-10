package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.CVConstants.LEARNING_RESOURCE;
import static io.harness.cvng.CVConstants.SERVICE_BASE_URL;
import static io.harness.cvng.CVConstants.TIMESERIES_ANALYSIS_RESOURCE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.ServiceGuardMetricAnalysisDTO;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.cvng.analysis.beans.TimeSeriesTestDataDTO;
import io.harness.cvng.analysis.entities.LearningEngineTask.LearningEngineTaskType;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns.TimeSeriesAnomalousPatternsKeys;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums.TimeSeriesCumulativeSumsKeys;
import io.harness.cvng.analysis.entities.TimeSeriesLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory.TimeSeriesShortTermHistoryKeys;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.entities.TimeSeriesRecord.TimeSeriesRecordKeys;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.mongodb.morphia.query.Query;

import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimeSeriesAnalysisServiceImpl implements TimeSeriesAnalysisService {
  @Inject private HPersistence hPersistence;
  @Inject private LearningEngineTaskService learningEngineTaskService;
  @Inject private TimeSeriesService timeSeriesService;
  @Inject private HeatMapService heatMapService;
  @Inject private CVConfigService cvConfigService;

  @Override
  public List<String> scheduleAnalysis(String cvConfigId, AnalysisInput analysisInput) {
    // create timeseries analysis task and schedule it
    TimeSeriesLearningEngineTask timeSeriesTask = createTimeSeriesLearningTask(analysisInput);

    learningEngineTaskService.createLearningEngineTasks(Arrays.asList(timeSeriesTask));

    // TODO: find a good way to return all taskIDs
    return Arrays.asList(timeSeriesTask.getUuid());
  }

  private TimeSeriesLearningEngineTask createTimeSeriesLearningTask(AnalysisInput input) {
    String taskId = generateUuid();
    int length = (int) Duration
                     .between(input.getStartTime().truncatedTo(ChronoUnit.SECONDS),
                         input.getEndTime().truncatedTo(ChronoUnit.SECONDS))
                     .toMinutes();

    TimeSeriesLearningEngineTask timeSeriesLearningEngineTask =
        TimeSeriesLearningEngineTask.builder()
            .cumulativeSumsUrl(createCumulativeSumsUrl(input))
            .dataLength(length)
            .keyTransactions(null)
            .metricTemplateUrl(createMetricTemplateUrl(input))
            .previousAnalysisUrl(createPreviousAnalysisUrl(input))
            .previousAnomaliesUrl(createAnomaliesUrl(input))
            .testDataUrl(createTestDataUrl(input))
            .build();

    timeSeriesLearningEngineTask.setCvConfigId(input.getCvConfigId());
    timeSeriesLearningEngineTask.setAnalysisType(LearningEngineTaskType.SERVICE_GUARD_TIME_SERIES);
    timeSeriesLearningEngineTask.setAnalysisStartTime(input.getStartTime());
    timeSeriesLearningEngineTask.setAnalysisEndTime(input.getEndTime());
    timeSeriesLearningEngineTask.setAnalysisEndEpochMinute(
        TimeUnit.MILLISECONDS.toMinutes(input.getEndTime().toEpochMilli()));
    timeSeriesLearningEngineTask.setAnalysisSaveUrl(createAnalysisSaveUrl(input, taskId));
    timeSeriesLearningEngineTask.setFailureUrl(createFailureUrl(taskId));
    timeSeriesLearningEngineTask.setUuid(taskId);

    return timeSeriesLearningEngineTask;
  }
  private String createAnalysisSaveUrl(AnalysisInput input, String taskId) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-save-analysis");
    uriBuilder.addParameter("taskId", taskId);
    uriBuilder.addParameter("cvConfigId", input.getCvConfigId());
    uriBuilder.addParameter("analysisStartTime", input.getStartTime().toString());
    uriBuilder.addParameter("analysisEndTime", input.getEndTime().toString());
    return getUriString(uriBuilder);
  }

  private String createFailureUrl(String taskId) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + LEARNING_RESOURCE + "/mark-failure");
    uriBuilder.addParameter("taskId", taskId);
    return getUriString(uriBuilder);
  }

  private String createPreviousAnalysisUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-shortterm-history");
    uriBuilder.addParameter("cvConfigId", input.getCvConfigId());
    return getUriString(uriBuilder);
  }

  private String createCumulativeSumsUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-cumulative-sums");
    uriBuilder.addParameter("cvConfigId", input.getCvConfigId());
    uriBuilder.addParameter("analysisStartTime", input.getStartTime().toString());
    uriBuilder.addParameter("analysisEndTime", input.getEndTime().toString());
    return getUriString(uriBuilder);
  }

  private String createAnomaliesUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-previous-anomalies");
    uriBuilder.addParameter("cvConfigId", input.getCvConfigId());
    return getUriString(uriBuilder);
  }

  private String createTestDataUrl(AnalysisInput input) {
    Instant startForTestData = input.getEndTime().truncatedTo(ChronoUnit.SECONDS).minus(125, ChronoUnit.MINUTES);

    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-test-data");
    uriBuilder.addParameter("cvConfigId", input.getCvConfigId());
    uriBuilder.addParameter("analysisStartTime", startForTestData.toString());
    uriBuilder.addParameter("analysisEndTime", input.getEndTime().toString());
    return getUriString(uriBuilder);
  }

  private String createMetricTemplateUrl(AnalysisInput input) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        SERVICE_BASE_URL + "/" + TIMESERIES_ANALYSIS_RESOURCE + "/timeseries-serviceguard-metric-template");
    uriBuilder.addParameter("cvConfigId", input.getCvConfigId());
    return getUriString(uriBuilder);
  }

  private String getUriString(URIBuilder uriBuilder) {
    try {
      return uriBuilder.build().toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Map<String, ExecutionStatus> getTaskStatus(String cvConfigId, Set<String> taskIds) {
    return learningEngineTaskService.getTaskStatus(taskIds);
  }

  @Override
  public Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> getCumulativeSums(
      String cvConfigId, Instant startTime, Instant endTime) {
    logger.info("Fetching cumulative sums for config: {}, startTime: {}, endTime: {}", cvConfigId, startTime, endTime);
    TimeSeriesCumulativeSums cumulativeSums = hPersistence.createQuery(TimeSeriesCumulativeSums.class)
                                                  .filter(TimeSeriesCumulativeSumsKeys.cvConfigId, cvConfigId)
                                                  .filter(TimeSeriesCumulativeSumsKeys.analysisStartTime, startTime)
                                                  .filter(TimeSeriesCumulativeSumsKeys.analysisEndTime, endTime)
                                                  .get();

    if (cumulativeSums != null) {
      return cumulativeSums.convertToMap();
    }
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Map<String, List<Double>>> getTestData(String cvConfigId, Instant startTime, Instant endTime) {
    TimeSeriesTestDataDTO testDataDTO = getTimeSeriesDataForRange(cvConfigId, startTime, endTime, null, null);

    return testDataDTO.getTransactionMetricValues();
  }

  private Map<String, Map<String, List<Double>>> getSortedListOfTimeSeriesRecords(
      Map<String, List<TimeSeriesRecord.TimeSeriesGroupValue>> unsortedTimeseries) {
    if (isNotEmpty(unsortedTimeseries)) {
      Map<String, Map<String, List<TimeSeriesRecord.TimeSeriesGroupValue>>> txnMetricMap = new HashMap<>();

      // first build the txn -> metric -> TimeSeriesGroupValue object
      unsortedTimeseries.forEach((metricName, txnValList) -> {
        txnValList.forEach(txnValue -> {
          String txnName = txnValue.getGroupName();
          if (!txnMetricMap.containsKey(txnName)) {
            txnMetricMap.put(txnName, new HashMap<>());
          }
          if (!txnMetricMap.get(txnName).containsKey(metricName)) {
            txnMetricMap.get(txnName).put(metricName, new ArrayList<>());
          }

          txnMetricMap.get(txnName).get(metricName).add(txnValue);
        });
      });

      // next sort the list under each txn->metric
      Map<String, Map<String, List<Double>>> txnMetricValueMap = new HashMap<>();
      for (String txnName : txnMetricMap.keySet()) {
        Map<String, List<TimeSeriesRecord.TimeSeriesGroupValue>> metricValueMap = txnMetricMap.get(txnName);
        txnMetricValueMap.put(txnName, new HashMap<>());

        for (String metricName : metricValueMap.keySet()) {
          List<TimeSeriesRecord.TimeSeriesGroupValue> valueList = metricValueMap.get(metricName);
          Collections.sort(valueList);
          txnMetricValueMap.get(txnName).put(metricName, new ArrayList<>());
          valueList.forEach(value -> { txnMetricValueMap.get(txnName).get(metricName).add(value.getMetricValue()); });
        }
      }

      return txnMetricValueMap;
    }
    return null;
  }

  @Override
  public TimeSeriesTestDataDTO getTimeSeriesDataForRange(
      String cvConfigId, Instant startTime, Instant endTime, String metricName, String txnName) {
    Instant queryStartTime = startTime.truncatedTo(ChronoUnit.SECONDS);
    Instant queryEndTime = endTime.truncatedTo(ChronoUnit.SECONDS);
    Query<TimeSeriesRecord> timeSeriesRecordsQuery = hPersistence.createQuery(TimeSeriesRecord.class, excludeAuthority)
                                                         .filter(TimeSeriesRecordKeys.cvConfigId, cvConfigId)
                                                         .field(TimeSeriesRecordKeys.bucketStartTime)
                                                         .greaterThanOrEq(queryStartTime)
                                                         .field(TimeSeriesRecordKeys.bucketStartTime)
                                                         .lessThan(queryEndTime);
    if (isNotEmpty(metricName)) {
      timeSeriesRecordsQuery = timeSeriesRecordsQuery.filter(TimeSeriesRecordKeys.metricName, metricName);
    }

    List<TimeSeriesRecord> records = timeSeriesRecordsQuery.asList();

    Map<String, List<TimeSeriesRecord.TimeSeriesGroupValue>> metricValueList = new HashMap<>();
    records.forEach(record -> {
      if (!metricValueList.containsKey(record.getMetricName())) {
        metricValueList.put(record.getMetricName(), new ArrayList<>());
      }

      List<TimeSeriesRecord.TimeSeriesGroupValue> valueList = metricValueList.get(record.getMetricName());
      List<TimeSeriesRecord.TimeSeriesGroupValue> curValueList = new ArrayList<>();
      // if txnName filter is present, filter by that name
      if (isNotEmpty(txnName)) {
        record.getTimeSeriesGroupValues().forEach(timeSeriesGroupValue -> {
          if (timeSeriesGroupValue.getGroupName().equals(txnName)) {
            curValueList.add(timeSeriesGroupValue);
          }
        });
      } else {
        curValueList.addAll(record.getTimeSeriesGroupValues());
      }

      // filter for those timestamps that fall within the start and endTime
      curValueList.forEach(groupValue -> {
        boolean timestampInWindow =
            !(groupValue.getTimeStamp().isBefore(startTime) || groupValue.getTimeStamp().isAfter(endTime));
        if (timestampInWindow) {
          valueList.add(groupValue);
        }
      });

      metricValueList.put(record.getMetricName(), valueList);
    });

    Map<String, Map<String, List<Double>>> sortedValueMap = getSortedListOfTimeSeriesRecords(metricValueList);

    return TimeSeriesTestDataDTO.builder().cvConfigId(cvConfigId).transactionMetricValues(sortedValueMap).build();
  }

  @Override
  public Map<String, Map<String, List<TimeSeriesAnomalies>>> getLongTermAnomalies(String cvConfigId) {
    logger.info("Fetching longterm anomalies for config: {}", cvConfigId);
    TimeSeriesAnomalousPatterns anomalousPatterns = hPersistence.createQuery(TimeSeriesAnomalousPatterns.class)
                                                        .filter(TimeSeriesAnomalousPatternsKeys.cvConfigId, cvConfigId)
                                                        .get();
    if (anomalousPatterns != null) {
      return anomalousPatterns.convertToMap();
    }
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Map<String, List<Double>>> getShortTermHistory(String cvConfigId) {
    logger.info("Fetching short term history for config: {}", cvConfigId);
    TimeSeriesShortTermHistory shortTermHistory = hPersistence.createQuery(TimeSeriesShortTermHistory.class)
                                                      .filter(TimeSeriesShortTermHistoryKeys.cvConfigId, cvConfigId)
                                                      .get();
    if (shortTermHistory != null) {
      return shortTermHistory.convertToMap();
    }
    return Collections.emptyMap();
  }

  @Override
  public void saveAnalysis(
      ServiceGuardMetricAnalysisDTO analysis, String cvConfigId, String taskId, Instant startTime, Instant endTime) {
    TimeSeriesShortTermHistory shortTermHistory = buildShortTermHistory(analysis);
    TimeSeriesCumulativeSums cumulativeSums = buildCumulativeSums(analysis, startTime, endTime);

    saveShortTermHistory(shortTermHistory);
    saveAnomalousPatterns(analysis, cvConfigId);
    hPersistence.save(cumulativeSums);
    logger.info("Saving analysis for config: {}", cvConfigId);
    learningEngineTaskService.markCompleted(taskId);
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    if (cvConfig != null) {
      double risk = analysis.getOverallMetricScores().values().stream().mapToDouble(score -> score).max().orElse(0.0);
      heatMapService.updateRiskScore(cvConfig.getAccountId(), cvConfig.getServiceIdentifier(),
          cvConfig.getEnvIdentifier(), CVMonitoringCategory.PERFORMANCE, endTime, risk);
    }
  }

  private void saveAnomalousPatterns(ServiceGuardMetricAnalysisDTO analysis, String cvConfigId) {
    TimeSeriesAnomalousPatterns patternsToSave = buildAnomalies(analysis);
    TimeSeriesAnomalousPatterns patternsFromDB = hPersistence.createQuery(TimeSeriesAnomalousPatterns.class)
                                                     .filter(TimeSeriesAnomalousPatternsKeys.cvConfigId, cvConfigId)
                                                     .get();

    if (patternsFromDB != null) {
      patternsToSave.setUuid(patternsFromDB.getUuid());
    }
    hPersistence.save(patternsToSave);
  }

  private void saveShortTermHistory(TimeSeriesShortTermHistory shortTermHistory) {
    TimeSeriesShortTermHistory historyFromDB =
        hPersistence.createQuery(TimeSeriesShortTermHistory.class)
            .filter(TimeSeriesShortTermHistoryKeys.cvConfigId, shortTermHistory.getCvConfigId())
            .get();
    if (historyFromDB != null) {
      shortTermHistory.setUuid(historyFromDB.getUuid());
    }
    hPersistence.save(shortTermHistory);
  }

  @Override
  public List<TimeSeriesMetricDefinition> getMetricTemplate(String cvConfigId) {
    return timeSeriesService.getTimeSeriesMetricDefinitions(cvConfigId);
  }

  private TimeSeriesCumulativeSums buildCumulativeSums(
      ServiceGuardMetricAnalysisDTO analysisDTO, Instant startTime, Instant endTime) {
    Map<String, Map<String, TimeSeriesCumulativeSums.MetricSum>> cumulativeSumsMap = new HashMap<>();
    analysisDTO.getTxnMetricAnalysisData().forEach((txnName, metricMap) -> {
      cumulativeSumsMap.put(txnName, new HashMap<>());
      metricMap.forEach((metricName, metricSums) -> {
        TimeSeriesCumulativeSums.MetricSum sums = metricSums.getCumulativeSums();
        sums.setMetricName(metricName);
        cumulativeSumsMap.get(txnName).put(metricName, sums);
      });
    });

    List<TimeSeriesCumulativeSums.TransactionMetricSums> transactionMetricSums =
        TimeSeriesCumulativeSums.convertMapToTransactionMetricSums(cumulativeSumsMap);

    return TimeSeriesCumulativeSums.builder()
        .cvConfigId(analysisDTO.getCvConfigId())
        .transactionMetricSums(transactionMetricSums)
        .analysisStartTime(startTime)
        .analysisEndTime(endTime)
        .build();
  }

  private TimeSeriesShortTermHistory buildShortTermHistory(ServiceGuardMetricAnalysisDTO analysisDTO) {
    Map<String, Map<String, List<Double>>> shortTermHistoryMap = new HashMap<>();
    analysisDTO.getTxnMetricAnalysisData().forEach((txnName, metricMap) -> {
      shortTermHistoryMap.put(txnName, new HashMap<>());
      metricMap.forEach((metricName, txnMetricData) -> {
        shortTermHistoryMap.get(txnName).put(metricName, txnMetricData.getShortTermHistory());
      });
    });

    return TimeSeriesShortTermHistory.builder()
        .cvConfigId(analysisDTO.getCvConfigId())
        .transactionMetricHistories(TimeSeriesShortTermHistory.convertFromMap(shortTermHistoryMap))
        .build();
  }

  private TimeSeriesAnomalousPatterns buildAnomalies(ServiceGuardMetricAnalysisDTO analysisDTO) {
    Map<String, Map<String, List<TimeSeriesAnomalies>>> anomaliesMap = new HashMap<>();
    analysisDTO.getTxnMetricAnalysisData().forEach((txnName, metricMap) -> {
      anomaliesMap.put(txnName, new HashMap<>());
      metricMap.forEach((metricName, txnMetricData) -> {
        anomaliesMap.get(txnName).put(metricName, txnMetricData.getAnomalousPatterns());
      });
    });

    return TimeSeriesAnomalousPatterns.builder()
        .cvConfigId(analysisDTO.getCvConfigId())
        .anomalies(TimeSeriesAnomalousPatterns.convertFromMap(anomaliesMap))
        .build();
  }
}
