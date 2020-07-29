package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.CVConstants.SERVICE_BASE_URL;
import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_ANALYSIS_RESOURCE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

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
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary.TransactionMetricRisk;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory.TimeSeriesShortTermHistoryKeys;
import io.harness.cvng.analysis.services.api.LearningEngineTaskService;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.dashboard.entities.Anomaly;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

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
  @Inject private AnomalyService anomalyService;

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
    timeSeriesLearningEngineTask.setFailureUrl(learningEngineTaskService.createFailureUrl(taskId));
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
    TimeSeriesTestDataDTO testDataDTO =
        timeSeriesService.getTxnMetricDataForRange(cvConfigId, startTime, endTime, null, null);

    return testDataDTO.getTransactionMetricValues();
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
    TimeSeriesRiskSummary riskSummary = buildRiskSummary(analysis, startTime, endTime);

    saveShortTermHistory(shortTermHistory);
    saveAnomalousPatterns(analysis, cvConfigId);
    hPersistence.save(riskSummary);
    hPersistence.save(cumulativeSums);
    logger.info("Saving analysis for config: {}", cvConfigId);
    learningEngineTaskService.markCompleted(taskId);
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    if (cvConfig != null) {
      double risk = analysis.getOverallMetricScores().values().stream().mapToDouble(score -> score).max().orElse(0.0);
      heatMapService.updateRiskScore(cvConfig.getAccountId(), cvConfig.getServiceIdentifier(),
          cvConfig.getEnvIdentifier(), CVMonitoringCategory.PERFORMANCE, endTime, risk);

      handleAnomalyOpenOrClose(cvConfig.getAccountId(), cvConfigId, startTime, endTime, risk, riskSummary);
    }
  }

  private void handleAnomalyOpenOrClose(String accountId, String cvConfigId, Instant startTime, Instant endTime,
      double overallRisk, TimeSeriesRiskSummary timeSeriesRiskSummary) {
    if (overallRisk <= 0.25) {
      anomalyService.closeAnomaly(accountId, cvConfigId, endTime);
    } else {
      if (timeSeriesRiskSummary != null) {
        List<TransactionMetricRisk> metricRisks = timeSeriesRiskSummary.getTransactionMetricRiskList();
        List<Anomaly.AnomalousMetric> anomalousMetrics = new ArrayList<>();
        metricRisks.forEach(metricRisk -> {
          if (metricRisk.getMetricRisk() > 0) {
            anomalousMetrics.add(Anomaly.AnomalousMetric.builder()
                                     .groupName(metricRisk.getTransactionName())
                                     .metricName(metricRisk.getMetricName())
                                     .riskScore(metricRisk.getMetricScore())
                                     .build());
          }
        });
        anomalyService.openAnomaly(accountId, cvConfigId, endTime, anomalousMetrics);
      }
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

  private TimeSeriesRiskSummary buildRiskSummary(
      ServiceGuardMetricAnalysisDTO analysisDTO, Instant startTime, Instant endTime) {
    List<TransactionMetricRisk> metricRiskList = new ArrayList<>();
    analysisDTO.getTxnMetricAnalysisData().forEach((txnName, metricMap) -> {
      metricMap.forEach((metricName, metricData) -> {
        TransactionMetricRisk metricRisk = TransactionMetricRisk.builder()
                                               .transactionName(txnName)
                                               .metricName(metricName)
                                               .metricRisk(metricData.getRisk())
                                               .metricScore(metricData.getScore())
                                               .lastSeenTime(metricData.getLastSeenTime())
                                               .longTermPattern(metricData.isLongTermPattern())
                                               .build();
        metricRiskList.add(metricRisk);
      });
    });
    return TimeSeriesRiskSummary.builder()
        .cvConfigId(analysisDTO.getCvConfigId())
        .analysisStartTime(startTime)
        .analysisEndTime(endTime)
        .transactionMetricRiskList(metricRiskList)
        .build();
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
