package io.harness.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.Integer.max;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.HARNESS_HEARTBEAT_METRIC_NAME;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.utils.Misc.replaceUnicodeWithDot;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.app.VerificationServiceConfiguration;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesAnomaliesRecord.TimeSeriesAnomaliesRecordKeys;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.entities.TimeSeriesCumulativeSums.TimeSeriesCumulativeSumsKeys;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.exception.WingsException;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.FeatureName;
import software.wings.common.VerificationConstants;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.MetricType;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.Threshold;
import software.wings.metrics.ThresholdCategory;
import software.wings.metrics.TimeSeriesDataRecord;
import software.wings.metrics.TimeSeriesDataRecord.TimeSeriesMetricRecordKeys;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.ExperimentStatus;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.MetricAnalysisRecord;
import software.wings.service.impl.analysis.MetricAnalysisRecord.MetricAnalysisRecordKeys;
import software.wings.service.impl.analysis.SupervisedTSThreshold;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLMetricScores;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMLScores.TimeSeriesMLScoresKeys;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMLTxnScores;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates.TimeSeriesMetricTemplatesKeys;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesRiskData;
import software.wings.service.impl.analysis.TimeSeriesRiskSummary;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord.NewRelicMetricDataRecordKeys;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by rsingh on 9/26/17.
 */
@Slf4j
public class TimeSeriesAnalysisServiceImpl implements TimeSeriesAnalysisService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private LearningEngineService learningEngineService;
  @Inject private VerificationManagerClient managerClient;
  @Inject private VerificationManagerClientHelper managerClientHelper;
  @Inject private DataStoreService dataStoreService;
  @Inject private VerificationServiceConfiguration verificationServiceConfiguration;
  @Inject private UsageMetricsHelper usageMetricsHelper;
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private DataCollectionExecutorService dataCollectionService;
  @Inject private CVActivityLogService cvActivityLogService;

  @Override
  @SuppressWarnings("PMD")
  public boolean saveMetricData(final String accountId, final String appId, final String stateExecutionId,
      String delegateTaskId, List<NewRelicMetricDataRecord> metricData) {
    if (isEmpty(metricData)) {
      logger.info("For state {} received empty collection", stateExecutionId);
      return false;
    }
    if (!learningEngineService.isStateValid(appId, stateExecutionId)) {
      logger.info("State is no longer active {}. Sending delegate abort request {}", stateExecutionId, delegateTaskId);
      return false;
    }
    metricData.forEach(metric -> {
      if (isNotEmpty(metric.getCvConfigId())) {
        metric.setValidUntil(Date.from(OffsetDateTime.now().plusMonths(1).toInstant()));
      }
    });
    dataStoreService.save(NewRelicMetricDataRecord.class, metricData, true);

    try {
      final List<TimeSeriesDataRecord> dataRecords =
          TimeSeriesDataRecord.getTimeSeriesDataRecordsFromNewRelicDataRecords(metricData);
      dataRecords.forEach(TimeSeriesDataRecord::compress);
      dataStoreService.save(TimeSeriesDataRecord.class, dataRecords, true);
    } catch (Throwable t) {
      logger.info("Error converting to TimeSeriesDataRecord", t);
    }
    if (stateExecutionId != null) {
      // TODO: get cvConfigId and dataCollectionMin and log when 24 * 7 config save happens
      cvActivityLogService.getLoggerByStateExecutionId(stateExecutionId)
          .info("Metrics data has been collected for minute " + getDataCollectionMinute(metricData));
    }
    return true;
  }

  private int getDataCollectionMinute(List<NewRelicMetricDataRecord> newRelicMetricDataRecords) {
    return newRelicMetricDataRecords.get(0).getDataCollectionMinute();
  }

  @Override
  public void saveAnalysisRecords(final NewRelicMetricAnalysisRecord metricAnalysisRecord) {
    wingsPersistence.save(metricAnalysisRecord);
    logger.info(
        "inserted NewRelicMetricAnalysisRecord to persistence layer for workflowExecutionId: {} StateExecutionInstanceId: {}",
        metricAnalysisRecord.getWorkflowExecutionId(), metricAnalysisRecord.getStateExecutionId());
    logLearningEngineAnalysisMessage(metricAnalysisRecord);
  }

  private void logLearningEngineAnalysisMessage(NewRelicMetricAnalysisRecord metricAnalysisRecord) {
    if (isNotEmpty(metricAnalysisRecord.getMessage())) {
      cvActivityLogService
          .getLogger(metricAnalysisRecord.getCvConfigId(), metricAnalysisRecord.getAnalysisMinute(),
              metricAnalysisRecord.getStateExecutionId())
          .warn(metricAnalysisRecord.getMessage());
    }
  }

  private void logLearningEngineAnalysisMessage(MetricAnalysisRecord mlAnalysisResponse) {
    if (isNotEmpty(mlAnalysisResponse.getMessage())) {
      cvActivityLogService
          .getLogger(mlAnalysisResponse.getCvConfigId(), mlAnalysisResponse.getAnalysisMinute(),
              mlAnalysisResponse.getStateExecutionId())
          .warn(mlAnalysisResponse.getMessage());
    }
  }
  /**
   * Method to save ml analysed records to mongoDB
   *
   *
   * @param accountId
   * @param stateType
   * @param appId
   * @param stateExecutionId
   * @param workflowExecutionId
   * @param groupName
   * @param analysisMinute
   * @param taskId
   * @param baseLineExecutionId
   * @param cvConfigId
   * @param mlAnalysisResponse
   * @return
   */
  @Override
  public boolean saveAnalysisRecordsML(String accountId, StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String groupName, Integer analysisMinute, String taskId, String baseLineExecutionId,
      String cvConfigId, MetricAnalysisRecord mlAnalysisResponse, String tag) {
    logger.info("saveAnalysisRecordsML stateType  {} stateExecutionId {} analysisMinute {}", stateType,
        stateExecutionId, analysisMinute);
    mlAnalysisResponse.setStateType(stateType);
    mlAnalysisResponse.setAppId(appId);
    mlAnalysisResponse.setWorkflowExecutionId(workflowExecutionId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    mlAnalysisResponse.setAnalysisMinute(analysisMinute);
    mlAnalysisResponse.setBaseLineExecutionId(baseLineExecutionId);
    mlAnalysisResponse.setGroupName(groupName);
    mlAnalysisResponse.setCvConfigId(cvConfigId);

    if (isEmpty(mlAnalysisResponse.getGroupName())) {
      mlAnalysisResponse.setGroupName(DEFAULT_GROUP_NAME);
    }

    TimeSeriesMLScores timeSeriesMLScores = TimeSeriesMLScores.builder()
                                                .appId(appId)
                                                .stateExecutionId(stateExecutionId)
                                                .workflowExecutionId(workflowExecutionId)
                                                .analysisMinute(analysisMinute)
                                                .stateType(stateType)
                                                .scoresMap(new HashMap<>())
                                                .build();
    int txnId = 0;
    int metricId;

    int aggregatedRisk = -1;

    TimeSeriesRiskSummary riskSummary =
        TimeSeriesRiskSummary.builder().analysisMinute(analysisMinute).cvConfigId(cvConfigId).build();

    riskSummary.setAppId(appId);
    TreeBasedTable<String, String, Integer> risks = TreeBasedTable.create();
    TreeBasedTable<String, String, Integer> longTermPatterns = TreeBasedTable.create();
    TreeBasedTable<String, String, TimeSeriesRiskData> riskData = TreeBasedTable.create();
    for (TimeSeriesMLTxnSummary txnSummary : mlAnalysisResponse.getTransactions().values()) {
      TimeSeriesMLTxnScores txnScores =
          TimeSeriesMLTxnScores.builder().transactionName(txnSummary.getTxn_name()).scoresMap(new HashMap<>()).build();
      timeSeriesMLScores.getScoresMap().put(String.valueOf(txnId), txnScores);

      metricId = 0;
      for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
        if (mlMetricSummary.getResults() != null) {
          TimeSeriesMLMetricScores mlMetricScores = TimeSeriesMLMetricScores.builder()
                                                        .metricName(mlMetricSummary.getMetric_name())
                                                        .scores(new ArrayList<>())
                                                        .build();
          txnScores.getScoresMap().put(String.valueOf(metricId), mlMetricScores);

          Iterator<Entry<String, TimeSeriesMLHostSummary>> resultsItr =
              mlMetricSummary.getResults().entrySet().iterator();
          Map<String, TimeSeriesMLHostSummary> timeSeriesMLHostSummaryMap = new HashMap<>();
          while (resultsItr.hasNext()) {
            Entry<String, TimeSeriesMLHostSummary> pair = resultsItr.next();
            timeSeriesMLHostSummaryMap.put(pair.getKey().replaceAll("\\.", "-"), pair.getValue());
            mlMetricScores.getScores().add(pair.getValue().getScore());
          }
          mlMetricSummary.setResults(timeSeriesMLHostSummaryMap);
          ++metricId;

          int maxRisk = mlMetricSummary.getMax_risk();
          int longTermPattern = mlMetricSummary.getLong_term_pattern();
          long lastSeenTime = mlMetricSummary.getLast_seen_time();
          riskData.put(txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(),
              TimeSeriesRiskData.builder()
                  .longTermPattern(longTermPattern)
                  .lastSeenTime(lastSeenTime)
                  .metricRisk(maxRisk)
                  .build());

          risks.put(txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(), mlMetricSummary.getMax_risk());
          longTermPatterns.put(
              txnSummary.getTxn_name(), mlMetricSummary.getMetric_name(), mlMetricSummary.getLong_term_pattern());
          aggregatedRisk = max(aggregatedRisk, mlMetricSummary.getMax_risk());
        }
      }
      ++txnId;
    }
    if (isNotEmpty(tag)) {
      mlAnalysisResponse.setTag(tag);
      riskSummary.setTag(tag);
    }
    riskSummary.setTxnMetricRisk(risks.rowMap());
    riskSummary.setTxnMetricLongTermPattern(longTermPatterns.rowMap());
    riskSummary.setTxnMetricRiskData(riskData.rowMap());
    riskSummary.compressMaps();

    mlAnalysisResponse.setAggregatedRisk(aggregatedRisk);
    mlAnalysisResponse.compressTransactions();

    if (mlAnalysisResponse instanceof ExperimentalMetricAnalysisRecord) {
      ((ExperimentalMetricAnalysisRecord) mlAnalysisResponse).setExperimentStatus(ExperimentStatus.UNDETERMINED);
      learningEngineService.markExpTaskCompleted(taskId);
      wingsPersistence.save(mlAnalysisResponse);
      try {
        managerClientHelper.callManagerWithRetry(
            managerClient.updateMismatchStatusInExperiment(stateExecutionId, analysisMinute));
      } catch (Exception e) {
        logger.info("Exception while updating mismatch status {}", mlAnalysisResponse.getStateExecutionId(), e);
      }
      return true;
    } else {
      saveTimeSeriesMLScores(timeSeriesMLScores);
      bumpCollectionMinuteToProcess(appId, stateExecutionId, workflowExecutionId, groupName, analysisMinute, accountId);
      learningEngineService.markCompleted(taskId);
    }

    if (isNotEmpty(mlAnalysisResponse.getAnomalies())) {
      TimeSeriesAnomaliesRecord anomaliesRecord = wingsPersistence.createQuery(TimeSeriesAnomaliesRecord.class)
                                                      .filter("appId", appId)
                                                      .filter(TimeSeriesAnomaliesRecordKeys.cvConfigId, cvConfigId)
                                                      .get();

      if (anomaliesRecord == null) {
        anomaliesRecord = TimeSeriesAnomaliesRecord.builder().cvConfigId(cvConfigId).build();
        anomaliesRecord.setAppId(appId);
        anomaliesRecord.setAnomalies(mlAnalysisResponse.getAnomalies());
      }

      anomaliesRecord.decompressAnomalies();

      if (anomaliesRecord.getAnomalies() == null) {
        anomaliesRecord.setAnomalies(new HashMap<>());
      }

      for (Entry<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies :
          mlAnalysisResponse.getAnomalies().entrySet()) {
        if (!anomaliesRecord.getAnomalies().containsKey(anomalies.getKey())) {
          anomaliesRecord.getAnomalies().put(anomalies.getKey(), anomalies.getValue());
        } else {
          for (Entry<String, List<TimeSeriesMLHostSummary>> metricAnomalies : anomalies.getValue().entrySet()) {
            anomaliesRecord.getAnomalies()
                .get(anomalies.getKey())
                .put(metricAnomalies.getKey(), metricAnomalies.getValue());
          }
        }
      }

      anomaliesRecord.compressAnomalies();
      wingsPersistence.save(anomaliesRecord);
    }
    if (isNotEmpty(mlAnalysisResponse.getTransactionMetricSums())) {
      TimeSeriesCumulativeSums cumulativeSums =
          TimeSeriesCumulativeSums.builder()
              .analysisMinute(mlAnalysisResponse.getAnalysisMinute())
              .cvConfigId(mlAnalysisResponse.getCvConfigId())
              .transactionMetricSums(mlAnalysisResponse.getTransactionMetricSums())
              .build();
      cumulativeSums.setAppId(appId);
      if (isNotEmpty(tag)) {
        cumulativeSums.setTag(tag);
      }
      cumulativeSums.compressMetricSums();
      wingsPersistence.saveIgnoringDuplicateKeys(Arrays.asList(cumulativeSums));
    }
    wingsPersistence.save(mlAnalysisResponse);
    wingsPersistence.save(riskSummary);

    try {
      managerClientHelper.callManagerWithRetry(
          managerClient.updateMismatchStatusInExperiment(stateExecutionId, analysisMinute));
    } catch (Exception e) {
      logger.info("Exception while updating mismatch status {}", mlAnalysisResponse.getStateExecutionId(), e);
    }

    if (mlAnalysisResponse.getOverallMetricScores() == null) {
      mlAnalysisResponse.setOverallMetricScores(new HashMap<>());
    }
    double riskScore =
        mlAnalysisResponse.getOverallMetricScores().values().stream().mapToDouble(score -> score).max().orElse(0.0);
    continuousVerificationService.triggerTimeSeriesAlertIfNecessary(
        cvConfigId, riskScore, mlAnalysisResponse.getAnalysisMinute());
    logger.info("inserted MetricAnalysisRecord to persistence layer for "
            + "stateType: {}, workflowExecutionId: {} StateExecutionInstanceId: {}",
        stateType, workflowExecutionId, stateExecutionId);
    logLearningEngineAnalysisMessage(mlAnalysisResponse);
    return true;
  }

  @Override
  public List<TimeSeriesMLScores> getTimeSeriesMLScores(
      String appId, String workflowId, int analysisMinute, int limit) {
    List<String> workflowExecutionIds = getLastSuccessfulWorkflowExecutionIds(appId, workflowId, null);
    return wingsPersistence.createQuery(TimeSeriesMLScores.class)
        .filter("workflowId", workflowId)
        .filter("appId", appId)
        .filter(TimeSeriesMLScoresKeys.analysisMinute, analysisMinute)
        .field("workflowExecutionIds")
        .in(workflowExecutionIds)
        .order("-createdAt")
        .asList(new FindOptions().limit(limit));
  }

  @Override
  public void saveTimeSeriesMLScores(TimeSeriesMLScores scores) {
    wingsPersistence.save(scores);
  }

  /**
   * Method to fetch Experimental Analysis Record for timeseries data.
   * @param appId
   * @param stateExecutionId
   * @param workflowExecutionId
   * @return
   */
  @Override
  public List<ExperimentalMetricAnalysisRecord> getExperimentalAnalysisRecordsByNaturalKey(
      String appId, String stateExecutionId, String workflowExecutionId) {
    return wingsPersistence.createQuery(ExperimentalMetricAnalysisRecord.class)
        .filter("appId", appId)
        .filter("stateExecutionId", stateExecutionId)
        .filter("workflowExecutionId", workflowExecutionId)
        .asList();
  }

  @Override
  public Set<NewRelicMetricDataRecord> getRecords(String appId, String stateExecutionId, String groupName,
      Set<String> nodes, int analysisMinute, int analysisStartMinute) {
    PageRequest<NewRelicMetricDataRecord> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(NewRelicMetricDataRecordKeys.stateExecutionId, Operator.EQ, stateExecutionId)
            .addFilter(NewRelicMetricDataRecordKeys.groupName, Operator.EQ, groupName)
            .addFilter(NewRelicMetricDataRecordKeys.dataCollectionMinute, Operator.LT_EQ, analysisMinute)
            .addFilter(NewRelicMetricDataRecordKeys.dataCollectionMinute, Operator.GE, analysisStartMinute)
            .build();

    final PageResponse<NewRelicMetricDataRecord> results =
        dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
    return results.stream()
        .filter(dataRecord
            -> nodes.contains(dataRecord.getHost()) && !ClusterLevel.H0.equals(dataRecord.getLevel())
                && !ClusterLevel.HF.equals(dataRecord.getLevel()))
        .collect(Collectors.toSet());
  }

  @Override
  public Set<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      String appId, String workflowExecutionId, String groupName, int analysisMinute, int analysisStartMinute) {
    if (isEmpty(workflowExecutionId)) {
      return Collections.emptySet();
    }
    PageRequest<NewRelicMetricDataRecord> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(NewRelicMetricDataRecordKeys.workflowExecutionId, Operator.EQ, workflowExecutionId)
            .addFilter(NewRelicMetricDataRecordKeys.groupName, Operator.EQ, groupName)
            .addFilter(NewRelicMetricDataRecordKeys.dataCollectionMinute, Operator.LT_EQ, analysisMinute)
            .addFilter(NewRelicMetricDataRecordKeys.dataCollectionMinute, Operator.GE, analysisStartMinute)
            .build();

    final PageResponse<NewRelicMetricDataRecord> results =
        dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
    return results.stream()
        .filter(dataRecord
            -> !ClusterLevel.H0.equals(dataRecord.getLevel()) && !ClusterLevel.HF.equals(dataRecord.getLevel()))
        .collect(Collectors.toSet());
  }

  @Override
  public int getMaxControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName) {
    return getControlMinuteWithData(stateType, serviceId, workflowExecutionId, groupName, OrderType.DESC);
  }

  @Override
  public int getMinControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName) {
    return getControlMinuteWithData(stateType, serviceId, workflowExecutionId, groupName, OrderType.ASC);
  }

  private int getControlMinuteWithData(
      StateType stateType, String serviceId, String workflowExecutionId, String groupName, OrderType orderType) {
    PageRequest<NewRelicMetricDataRecord> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(NewRelicMetricDataRecordKeys.workflowExecutionId, Operator.EQ, workflowExecutionId)
            .build();

    PageResponse<NewRelicMetricDataRecord> results = dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
    if (results.isEmpty()) {
      return -1;
    }
    final List<NewRelicMetricDataRecord> records =
        results.getResponse()
            .stream()
            .filter(dataRecord
                -> dataRecord.getStateType().equals(stateType) && dataRecord.getServiceId().equals(serviceId)
                    && (dataRecord.getGroupName().equals(groupName)
                           || dataRecord.getGroupName().equals(DEFAULT_GROUP_NAME))
                    && (!ClusterLevel.H0.equals(dataRecord.getLevel())
                           && !ClusterLevel.HF.equals(dataRecord.getLevel())))
            .collect(Collectors.toList());
    if (records.isEmpty()) {
      return -1;
    }
    switch (orderType) {
      case ASC:
        Collections.sort(records, Comparator.comparingInt(NewRelicMetricDataRecord::getDataCollectionMinute));
        break;
      case DESC:
        Collections.sort(records, (o1, o2) -> o2.getDataCollectionMinute() - o1.getDataCollectionMinute());
        break;
      default:
        unhandled(orderType);
    }
    return records.get(0).getDataCollectionMinute();
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId) {
    List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(appId, workflowId, serviceId);
    for (String successfulExecution : successfulExecutions) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter(NewRelicMetricDataRecordKeys.workflowExecutionId, Operator.EQ, successfulExecution)
              .build();

      PageResponse<NewRelicMetricDataRecord> results =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
      if (results.isEmpty()) {
        continue;
      }
      final List<NewRelicMetricDataRecord> records =
          results.getResponse()
              .stream()
              .filter(dataRecord
                  -> dataRecord.getStateType().equals(stateType) && dataRecord.getServiceId().equals(serviceId)

                      && (!ClusterLevel.H0.equals(dataRecord.getLevel())
                             && !ClusterLevel.HF.equals(dataRecord.getLevel())))
              .collect(Collectors.toList());
      if (!records.isEmpty()) {
        return successfulExecution;
      }
    }
    logger.warn("Could not get a successful workflow to find control nodes");
    return null;
  }

  @Override
  public List<String> getLastSuccessfulWorkflowExecutionIds(String appId, String workflowId, String serviceId) {
    return managerClientHelper
        .callManagerWithRetry(managerClient.getLastSuccessfulWorkflowExecutionIds(appId, workflowId, serviceId))
        .getResource();
  }

  private RiskLevel getRiskLevel(int risk) {
    RiskLevel riskLevel;
    switch (risk) {
      case -1:
        riskLevel = RiskLevel.NA;
        break;
      case 0:
        riskLevel = RiskLevel.LOW;
        break;
      case 1:
        riskLevel = RiskLevel.MEDIUM;
        break;
      case 2:
        riskLevel = RiskLevel.HIGH;
        break;
      default:
        throw new IllegalArgumentException("Unknown risk level " + risk);
    }
    return riskLevel;
  }

  @Override
  @Deprecated
  public Map<String, Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(String appId, StateType stateType,
      String stateExecutionId, String serviceId, String cvConfigId, String groupName) {
    Map<String, Map<String, TimeSeriesMetricDefinition>> result = new HashMap<>();
    switch (stateType) {
      case NEW_RELIC:
        result.put("default", NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE);
        break;
      case DYNA_TRACE:
        result.put("default", DynaTraceTimeSeries.getDefinitionsToAnalyze());
        break;
      case APP_DYNAMICS:
      case PROMETHEUS:
      case CLOUD_WATCH:
      case DATA_DOG:
      case STACK_DRIVER:
      case APM_VERIFICATION:
        result.put("default", getMetricTemplates(appId, stateType, stateExecutionId, cvConfigId));
        break;
      default:
        throw new WingsException("Invalid Verification StateType.");
    }

    result.putAll(getCustomMetricTemplates(appId, stateType, serviceId, cvConfigId, groupName));

    return result;
  }

  @Override
  public Map<String, Map<String, TimeSeriesMetricDefinition>> getMetricTemplateWithCategorizedThresholds(String appId,
      StateType stateType, String stateExecutionId, String serviceId, String cvConfigId, String groupName) {
    Map<String, Map<String, TimeSeriesMetricDefinition>> result = new HashMap<>();

    // Add supervised metric thresholds
    addSupervisedMetricThresholds(serviceId, result);

    // Add user defined metric thresholds
    addUserDefinedMetricThresholds(appId, stateType, serviceId, cvConfigId, groupName, result);

    // Add default metrics under "default" key for all enabled metrics
    addDefaultMetricThresholds(stateType, appId, stateExecutionId, cvConfigId, result);

    return result;
  }

  @Override
  public NewRelicMetricDataRecord getHeartBeat(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName, OrderType orderType) {
    logger.info(
        "Querying for getLastHeartBeat. Params are: stateType {}, appId {}, stateExecutionId: {}, workflowExecutionId: {} serviceId {}, groupName: {} ",
        stateType, appId, stateExecutionId, workflowExecutionId, serviceId, groupName);

    PageRequest<NewRelicMetricDataRecord> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(NewRelicMetricDataRecordKeys.stateExecutionId, Operator.EQ, stateExecutionId)
            .addFilter(NewRelicMetricDataRecordKeys.groupName, Operator.EQ, groupName)
            .addOrder(NewRelicMetricDataRecordKeys.dataCollectionMinute, orderType)
            .build();

    final PageResponse<NewRelicMetricDataRecord> results =
        dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
    List<NewRelicMetricDataRecord> rv =
        results.stream()
            .filter(dataRecord
                -> dataRecord.getStateType().equals(stateType) && dataRecord.getServiceId().equals(serviceId)
                    && ClusterLevel.HF.equals(dataRecord.getLevel()))
            .collect(Collectors.toList());

    if (isEmpty(rv)) {
      logger.info(
          "No heartbeat record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}",
          ClusterLevel.HF, stateExecutionId, workflowExecutionId, serviceId);
      return null;
    }
    return rv.get(0);
  }

  @Override
  public NewRelicMetricDataRecord getAnalysisMinute(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName) {
    logger.info(
        "Querying for getLastHeartBeat. Params are: stateType {}, appId {}, stateExecutionId: {}, workflowExecutionId: {} serviceId {}, groupName: {} ",
        stateType, appId, stateExecutionId, workflowExecutionId, serviceId, groupName);
    PageRequest<NewRelicMetricDataRecord> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(NewRelicMetricDataRecordKeys.stateExecutionId, Operator.EQ, stateExecutionId)
            .addFilter(NewRelicMetricDataRecordKeys.groupName, Operator.EQ, groupName)
            .addOrder(NewRelicMetricDataRecordKeys.dataCollectionMinute, OrderType.DESC)
            .build();

    final PageResponse<NewRelicMetricDataRecord> results =
        dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
    List<NewRelicMetricDataRecord> rv =
        results.stream()
            .filter(dataRecord
                -> dataRecord.getStateType().equals(stateType) && dataRecord.getServiceId().equals(serviceId)
                    && ClusterLevel.H0.equals(dataRecord.getLevel()))
            .collect(Collectors.toList());

    if (isEmpty(rv)) {
      logger.info(
          "No metric record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}.",
          ClusterLevel.H0, stateExecutionId, workflowExecutionId, serviceId);
      return null;
    }
    logger.info("Returning the record: {} from getAnalysisMinute", rv.get(0));
    return rv.get(0);
  }

  @Override
  public void bumpCollectionMinuteToProcess(String appId, String stateExecutionId, String workflowExecutionId,
      String groupName, int analysisMinute, String accountId) {
    logger.info(
        "bumpCollectionMinuteToProcess. Going to update the record for stateExecutionId {} and dataCollectionMinute {}",
        stateExecutionId, analysisMinute);
    if (isEmpty(stateExecutionId)) {
      return;
    }

    if (managerClientHelper
            .callManagerWithRetry(
                managerClient.isFeatureEnabled(FeatureName.GDS_TIME_SERIES_SAVE_PER_MINUTE, accountId))
            .getResource()) {
      PageRequest<TimeSeriesDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter(TimeSeriesMetricRecordKeys.stateExecutionId, Operator.EQ, stateExecutionId)
              .addFilter(TimeSeriesMetricRecordKeys.groupName, Operator.EQ, groupName)
              .addFilter(TimeSeriesMetricRecordKeys.dataCollectionMinute, Operator.LT_EQ, analysisMinute)
              .addOrder(TimeSeriesMetricRecordKeys.dataCollectionMinute, OrderType.DESC)
              .build();
      final PageResponse<TimeSeriesDataRecord> dataRecords =
          dataStoreService.list(TimeSeriesDataRecord.class, pageRequest);

      dataRecords.stream()
          .filter(dataRecord -> ClusterLevel.H0.equals(dataRecord.getLevel()))
          .forEach(dataRecord -> dataRecord.setLevel(ClusterLevel.HF));
      dataStoreService.save(TimeSeriesDataRecord.class, dataRecords, false);
    } else {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter(NewRelicMetricDataRecordKeys.stateExecutionId, Operator.EQ, stateExecutionId)
              .addFilter(NewRelicMetricDataRecordKeys.groupName, Operator.EQ, groupName)
              .addFilter(NewRelicMetricDataRecordKeys.dataCollectionMinute, Operator.LT_EQ, analysisMinute)
              .addOrder(NewRelicMetricDataRecordKeys.dataCollectionMinute, OrderType.DESC)
              .build();
      final PageResponse<NewRelicMetricDataRecord> dataRecords =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);

      dataRecords.stream()
          .filter(dataRecord -> ClusterLevel.H0.equals(dataRecord.getLevel()))
          .forEach(dataRecord -> dataRecord.setLevel(ClusterLevel.HF));
      dataStoreService.save(NewRelicMetricDataRecord.class, dataRecords, false);
    }
  }

  @Override
  public Map<String, TimeSeriesMetricDefinition> getMetricTemplates(
      String appId, StateType stateType, String stateExecutionId, String cvConfigId) {
    TimeSeriesMetricTemplates newRelicMetricTemplates =
        wingsPersistence.createQuery(TimeSeriesMetricTemplates.class, excludeAuthority)
            .filter(TimeSeriesMetricTemplatesKeys.stateExecutionId, stateExecutionId)
            .filter(TimeSeriesMetricTemplatesKeys.cvConfigId, cvConfigId)
            .get();
    return newRelicMetricTemplates == null ? null : newRelicMetricTemplates.getMetricTemplates();
  }

  private Map<String, Map<String, TimeSeriesMetricDefinition>> getCustomMetricTemplates(
      String appId, StateType stateType, String serviceId, String cvConfigId, String groupName) {
    List<TimeSeriesMLTransactionThresholds> thresholds =
        wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
            .filter("appId", appId)
            .filter("serviceId", serviceId)
            .filter("stateType", stateType)
            .filter("groupName", groupName)
            .filter("cvConfigId", cvConfigId)
            .asList();
    Map<String, Map<String, TimeSeriesMetricDefinition>> customThresholds = new HashMap<>();
    if (thresholds != null) {
      for (TimeSeriesMLTransactionThresholds threshold : thresholds) {
        if (!customThresholds.containsKey(threshold.getTransactionName())) {
          customThresholds.put(threshold.getTransactionName(), new HashMap<>());
        }
        customThresholds.get(threshold.getTransactionName()).put(threshold.getMetricName(), threshold.getThresholds());
      }
    }
    return customThresholds;
  }

  private void addUserDefinedMetricThresholds(String appId, StateType stateType, String serviceId, String cvConfigId,
      String groupName, Map<String, Map<String, TimeSeriesMetricDefinition>> metricDefinitionMap) {
    List<TimeSeriesMLTransactionThresholds> thresholds =
        wingsPersistence.createQuery(TimeSeriesMLTransactionThresholds.class)
            .filter(TimeSeriesMLTransactionThresholds.BaseKeys.appId, appId)
            .filter(TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys.serviceId, serviceId)
            .filter(TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys.stateType, stateType)
            .filter(TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys.groupName, groupName)
            .filter(TimeSeriesMLTransactionThresholds.TimeSeriesMLTransactionThresholdKeys.cvConfigId, cvConfigId)
            .asList();
    if (thresholds != null) {
      for (TimeSeriesMLTransactionThresholds threshold : thresholds) {
        updateThresholdDefinitionSkeleton(threshold.getTransactionName(), threshold.getMetricName(),
            threshold.getThresholds().getMetricType(), metricDefinitionMap);
        List<Threshold> userDefinedThresholds = threshold.getThresholds().getCustomThresholds();
        metricDefinitionMap.get(threshold.getTransactionName())
            .get(threshold.getMetricName())
            .getCategorizedThresholds()
            .put(ThresholdCategory.USER_DEFINED, userDefinedThresholds);
      }
    }
  }

  private void addSupervisedMetricThresholds(
      String serviceId, Map<String, Map<String, TimeSeriesMetricDefinition>> metricDefinitionMap) {
    try {
      List<SupervisedTSThreshold> thresholds =
          dataStoreService
              .list(SupervisedTSThreshold.class, aPageRequest().addFilter("serviceId", Operator.EQ, serviceId).build())
              .getResponse();
      if (isNotEmpty(thresholds)) {
        for (SupervisedTSThreshold threshold : thresholds) {
          updateThresholdDefinitionSkeleton(threshold.getTransactionName(), threshold.getMetricName(),
              threshold.getMetricType(), metricDefinitionMap);
          List<Threshold> supervisedThresholds = SupervisedTSThreshold.getThresholds(threshold);
          metricDefinitionMap.get(threshold.getTransactionName())
              .get(threshold.getMetricName())
              .getCategorizedThresholds()
              .put(ThresholdCategory.SUPERVISED, supervisedThresholds);
        }
      }
    } catch (Exception e) {
      logger.error("Exception while fetching supervised metric thresholds", e);
    }
  }

  private void addDefaultMetricThresholds(StateType stateType, String appId, String stateExecutionId, String cvConfigId,
      Map<String, Map<String, TimeSeriesMetricDefinition>> metricDefinitionMap) {
    List<TimeSeriesMetricDefinition> metricDefinitions = new ArrayList<>();
    switch (stateType) {
      case NEW_RELIC:
        metricDefinitions.addAll(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE.values());
        break;
      case DYNA_TRACE:
        metricDefinitions.addAll(DynaTraceTimeSeries.getDefinitionsToAnalyze().values());
        break;
      case APP_DYNAMICS:
      case PROMETHEUS:
      case CLOUD_WATCH:
      case DATA_DOG:
      case STACK_DRIVER:
      case APM_VERIFICATION:
        metricDefinitions.addAll(getMetricTemplates(appId, stateType, stateExecutionId, cvConfigId).values());
        break;
      default:
        throw new WingsException("Invalid Verification StateType.");
    }

    for (TimeSeriesMetricDefinition metricDefinition : metricDefinitions) {
      updateThresholdDefinitionSkeleton(
          "default", metricDefinition.getMetricName(), metricDefinition.getMetricType(), metricDefinitionMap);
    }
  }

  private void updateThresholdDefinitionSkeleton(String transactionName, String metricName, MetricType metricType,
      Map<String, Map<String, TimeSeriesMetricDefinition>> metricDefinitionMap) {
    if (!metricDefinitionMap.containsKey(transactionName)) {
      metricDefinitionMap.put(transactionName, new HashMap<>());
    }
    if (!metricDefinitionMap.get(transactionName).containsKey(metricName)) {
      metricDefinitionMap.get(transactionName)
          .put(metricName,
              TimeSeriesMetricDefinition.builder()
                  .metricName(metricName)
                  .metricType(metricType)
                  .categorizedThresholds(new HashMap<>())
                  .build());
      metricDefinitionMap.get(transactionName)
          .get(metricName)
          .getCategorizedThresholds()
          .put(ThresholdCategory.DEFAULT, metricType.getThresholds());
    }
  }

  @Override
  public Map<String, TimeSeriesMlAnalysisGroupInfo> getMetricGroups(String appId, String stateExecutionId) {
    TimeSeriesMetricGroup timeSeriesMetricGroup = wingsPersistence.createQuery(TimeSeriesMetricGroup.class)
                                                      .field("stateExecutionId")
                                                      .equal(stateExecutionId)
                                                      .field("appId")
                                                      .equal(appId)
                                                      .get();

    if (timeSeriesMetricGroup != null) {
      Map<String, TimeSeriesMlAnalysisGroupInfo> toReturn = new HashMap<>();
      timeSeriesMetricGroup.getGroups().forEach((groupName, timeSeriesMlAnalysisGroupInfo) -> {
        groupName = replaceUnicodeWithDot(groupName);
        timeSeriesMlAnalysisGroupInfo.setGroupName(groupName);
        toReturn.put(groupName, timeSeriesMlAnalysisGroupInfo);
      });

      return toReturn;
    }

    return new ImmutableMap.Builder<String, TimeSeriesMlAnalysisGroupInfo>()
        .put(DEFAULT_GROUP_NAME,
            TimeSeriesMlAnalysisGroupInfo.builder()
                .groupName(DEFAULT_GROUP_NAME)
                .dependencyPath(DEFAULT_GROUP_NAME)
                .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
                .build())
        .build();
  }

  @VisibleForTesting
  public void saveMetricTemplates(String appId, StateType stateType, String stateExecutionId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates) {
    TimeSeriesMetricTemplates metricTemplate = TimeSeriesMetricTemplates.builder()
                                                   .stateType(stateType)
                                                   .stateExecutionId(stateExecutionId)
                                                   .metricTemplates(metricTemplates)
                                                   .build();
    metricTemplate.setAppId(appId);
    wingsPersistence.save(metricTemplate);
  }

  @Override
  public long getMaxCVCollectionMinute(String appId, String cvConfigId, String accountId) {
    if (managerClientHelper
            .callManagerWithRetry(
                managerClient.isFeatureEnabled(FeatureName.GDS_TIME_SERIES_SAVE_PER_MINUTE, accountId))
            .getResource()) {
      PageRequest<TimeSeriesDataRecord> pageRequest =
          aPageRequest()
              .withLimit("1")
              .addFilter(TimeSeriesMetricRecordKeys.cvConfigId, Operator.EQ, cvConfigId)
              .addOrder(TimeSeriesMetricRecordKeys.dataCollectionMinute, OrderType.DESC)
              .build();

      final PageResponse<TimeSeriesDataRecord> results = dataStoreService.list(TimeSeriesDataRecord.class, pageRequest);
      if (isEmpty(results)) {
        return -1;
      }
      return results.get(0).getDataCollectionMinute();
    } else {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit("1")
              .addFilter(NewRelicMetricDataRecordKeys.cvConfigId, Operator.EQ, cvConfigId)
              .addOrder(NewRelicMetricDataRecordKeys.dataCollectionMinute, OrderType.DESC)
              .build();

      final PageResponse<NewRelicMetricDataRecord> results =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
      if (isEmpty(results)) {
        return -1;
      }
      return results.get(0).getDataCollectionMinute();
    }
  }

  @Override
  public long getLastCVAnalysisMinute(String appId, String cvConfigId) {
    TimeSeriesMLAnalysisRecord newRelicMetricAnalysisRecord =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class, excludeAuthority)
            .filter(MetricAnalysisRecordKeys.cvConfigId, cvConfigId)
            .project(MetricAnalysisRecordKeys.analysisMinute, true)
            .order(Sort.descending(MetricAnalysisRecordKeys.analysisMinute))
            .get();
    return newRelicMetricAnalysisRecord == null ? -1 : newRelicMetricAnalysisRecord.getAnalysisMinute();
  }

  @Override
  public Set<NewRelicMetricDataRecord> getMetricRecords(
      String cvConfigId, int analysisStartMinute, int analysisEndMinute, String tag, String accountId) {
    ConcurrentMap<NewRelicMetricDataRecord, Boolean> rv = new ConcurrentHashMap<>();
    List<Callable<Void>> callables = new ArrayList<>();
    for (int startMin = analysisStartMinute; startMin <= analysisEndMinute;
         startMin = startMin + CRON_POLL_INTERVAL_IN_MINUTES) {
      final int endMin = startMin + CRON_POLL_INTERVAL_IN_MINUTES < analysisEndMinute
          ? startMin + CRON_POLL_INTERVAL_IN_MINUTES
          : analysisEndMinute;
      final int dataStartMin = startMin;
      callables.add(() -> {
        List<NewRelicMetricDataRecord> results;

        if (managerClientHelper
                .callManagerWithRetry(
                    managerClient.isFeatureEnabled(FeatureName.GDS_TIME_SERIES_SAVE_PER_MINUTE, accountId))
                .getResource()) {
          PageRequest<TimeSeriesDataRecord> pageRequest =
              aPageRequest()
                  .withLimit(UNLIMITED)
                  .addFilter(TimeSeriesMetricRecordKeys.cvConfigId, Operator.EQ, cvConfigId)
                  .addFilter(TimeSeriesMetricRecordKeys.dataCollectionMinute, Operator.GE, dataStartMin)
                  .addFilter(TimeSeriesMetricRecordKeys.dataCollectionMinute, Operator.LT_EQ, endMin)
                  .build();

          if (isNotEmpty(tag)) {
            pageRequest.addFilter(TimeSeriesMetricRecordKeys.tag, Operator.EQ, tag);
          }

          final PageResponse<TimeSeriesDataRecord> dataRecordKeys =
              dataStoreService.list(TimeSeriesDataRecord.class, pageRequest);

          results = TimeSeriesDataRecord.getNewRelicDataRecordsFromTimeSeriesDataRecords(dataRecordKeys.getResponse());
        } else {
          PageRequest<NewRelicMetricDataRecord> pageRequest =
              aPageRequest()
                  .withLimit(UNLIMITED)
                  .addFilter(NewRelicMetricDataRecordKeys.cvConfigId, Operator.EQ, cvConfigId)
                  .addFilter(NewRelicMetricDataRecordKeys.dataCollectionMinute, Operator.GE, dataStartMin)
                  .addFilter(NewRelicMetricDataRecordKeys.dataCollectionMinute, Operator.LT_EQ, endMin)
                  .build();

          if (isNotEmpty(tag)) {
            pageRequest.addFilter(NewRelicMetricDataRecordKeys.tag, Operator.EQ, tag);
          }

          results = dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
        }
        results.stream()
            .filter(dataRecord -> !dataRecord.getName().equals(HARNESS_HEARTBEAT_METRIC_NAME))
            .forEach(dataRecord -> rv.put(dataRecord, Boolean.TRUE));
        return null;
      });
    }

    dataCollectionService.executeParrallel(callables);
    return rv.keySet();
  }

  @Override
  public TimeSeriesMLAnalysisRecord getPreviousAnalysis(
      String appId, String cvConfigId, long dataCollectionMin, String tag) {
    Query<TimeSeriesMLAnalysisRecord> analysisQuery =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class, excludeAuthority)
            .filter(MetricAnalysisRecordKeys.cvConfigId, cvConfigId)
            .filter(MetricAnalysisRecordKeys.analysisMinute, dataCollectionMin);

    if (isNotEmpty(tag)) {
      analysisQuery = analysisQuery.filter(MetricAnalysisRecordKeys.tag, tag);
    }
    final TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord = analysisQuery.get();
    if (timeSeriesMLAnalysisRecord != null) {
      timeSeriesMLAnalysisRecord.decompressTransactions();
    }
    return timeSeriesMLAnalysisRecord;
  }

  @Override
  public List<TimeSeriesMLAnalysisRecord> getHistoricalAnalysis(
      String accountId, String appId, String serviceId, String cvConfigId, long analysisMin, String tag) {
    List<Long> historicalAnalysisTimes = new ArrayList<>();

    for (int i = 1; i <= VerificationConstants.CANARY_DAYS_TO_COLLECT; i++) {
      historicalAnalysisTimes.add(analysisMin - i * TimeUnit.DAYS.toMinutes(7));
    }

    if (historicalAnalysisTimes.size() == 0) {
      return new ArrayList<>();
    }

    Query<TimeSeriesMLAnalysisRecord> analysisQuery =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class, excludeAuthority)
            .filter(MetricAnalysisRecordKeys.cvConfigId, cvConfigId)
            .field(MetricAnalysisRecordKeys.analysisMinute)
            .in(historicalAnalysisTimes);

    if (isNotEmpty(tag)) {
      analysisQuery = analysisQuery.filter(MetricAnalysisRecordKeys.tag, tag);
    }

    final List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = analysisQuery.asList();
    if (timeSeriesMLAnalysisRecords != null) {
      timeSeriesMLAnalysisRecords.forEach(
          timeSeriesMLAnalysisRecord -> timeSeriesMLAnalysisRecord.decompressTransactions());
    }
    return timeSeriesMLAnalysisRecords;
  }

  @Override
  public TimeSeriesAnomaliesRecord getPreviousAnomalies(
      String appId, String cvConfigId, Map<String, List<String>> metrics, String tag) {
    Query<TimeSeriesAnomaliesRecord> timeSeriesAnomaliesRecordQuery =
        wingsPersistence.createQuery(TimeSeriesAnomaliesRecord.class)
            .filter(MetricAnalysisRecordKeys.cvConfigId, cvConfigId);
    if (isNotEmpty(tag)) {
      timeSeriesAnomaliesRecordQuery = timeSeriesAnomaliesRecordQuery.filter("tag", tag);
    }
    TimeSeriesAnomaliesRecord timeSeriesAnomaliesRecord = timeSeriesAnomaliesRecordQuery.get();

    if (timeSeriesAnomaliesRecord == null) {
      return null;
    }

    timeSeriesAnomaliesRecord.decompressAnomalies();

    if (isEmpty(metrics)) {
      return timeSeriesAnomaliesRecord;
    }

    final Map<String, Map<String, List<TimeSeriesMLHostSummary>>> anomalies = new HashMap<>();
    metrics.forEach((txnName, metricNames) -> {
      if (timeSeriesAnomaliesRecord.getAnomalies().containsKey(txnName)) {
        final Map<String, List<TimeSeriesMLHostSummary>> metricAnomalies =
            timeSeriesAnomaliesRecord.getAnomalies().get(txnName);
        metricNames.forEach(metricName -> {
          if (metricAnomalies.containsKey(metricName)) {
            if (!anomalies.containsKey(txnName)) {
              anomalies.put(txnName, new HashMap<>());
            }
            anomalies.get(txnName).put(metricName, metricAnomalies.get(metricName));
          }
        });
      }
    });

    timeSeriesAnomaliesRecord.setAnomalies(anomalies);
    return timeSeriesAnomaliesRecord;
  }

  @Override
  public Set<TimeSeriesCumulativeSums> getCumulativeSumsForRange(
      String appId, String cvConfigId, int startMinute, int endMinute, String tag) {
    if (isNotEmpty(appId) && isNotEmpty(cvConfigId) && startMinute <= endMinute) {
      Query<TimeSeriesCumulativeSums> timeSeriesCumulativeSumsQuery =
          wingsPersistence.createQuery(TimeSeriesCumulativeSums.class, excludeAuthority)
              .filter(TimeSeriesCumulativeSumsKeys.cvConfigId, cvConfigId)
              .field(TimeSeriesCumulativeSumsKeys.analysisMinute)
              .greaterThanOrEq(startMinute)
              .field(TimeSeriesCumulativeSumsKeys.analysisMinute)
              .lessThanOrEq(endMinute);
      if (isNotEmpty(tag)) {
        timeSeriesCumulativeSumsQuery = timeSeriesCumulativeSumsQuery.filter(TimeSeriesCumulativeSumsKeys.tag, tag);
      }
      List<TimeSeriesCumulativeSums> cumulativeSums = timeSeriesCumulativeSumsQuery.asList();

      logger.info(
          "Returning a list of size {} from getCumulativeSumsForRange for appId {}, cvConfigId {} and start {} and end {}",
          cumulativeSums.size(), appId, cvConfigId, startMinute, endMinute);
      cumulativeSums.forEach(metricSum -> metricSum.decompressMetricSums());
      return Sets.newHashSet(cumulativeSums);
    } else {
      final String errorMsg = "AppId or CVConfigId is null in getCumulativeSumsForRange";
      logger.error(errorMsg);
      throw new WingsException(errorMsg);
    }
  }

  @Override
  public long getLastDataCollectedMinute(String appId, String stateExecutionId, StateType stateType) {
    NewRelicMetricDataRecord newRelicMetricDataRecord =
        wingsPersistence.createQuery(NewRelicMetricDataRecord.class, excludeAuthority)
            .filter(NewRelicMetricDataRecordKeys.stateExecutionId, stateExecutionId)
            .filter(NewRelicMetricDataRecordKeys.stateType, stateType)
            .project(NewRelicMetricDataRecordKeys.dataCollectionMinute, true)
            .order(Sort.descending(NewRelicMetricDataRecordKeys.dataCollectionMinute))
            .get();
    return newRelicMetricDataRecord == null ? -1 : newRelicMetricDataRecord.getDataCollectionMinute();
  }
}
