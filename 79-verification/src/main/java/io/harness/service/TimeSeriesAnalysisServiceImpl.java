package io.harness.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeCount;
import static java.lang.Integer.max;
import static java.util.Arrays.asList;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.common.VerificationConstants.MEDIUM_RISK_CUTOFF;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.HARNESS_HEARTBEAT_METRIC_NAME;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.utils.Misc.replaceUnicodeWithDot;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.app.VerificationServiceConfiguration;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.exception.WingsException;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.DataStorageMode;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.MetricAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLMetricScores;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMLTxnScores;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesRiskData;
import software.wings.service.impl.analysis.TimeSeriesRiskSummary;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by rsingh on 9/26/17.
 */
public class TimeSeriesAnalysisServiceImpl implements TimeSeriesAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(TimeSeriesAnalysisServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private LearningEngineService learningEngineService;
  @Inject private VerificationManagerClient managerClient;
  @Inject private VerificationManagerClientHelper managerClientHelper;
  @Inject private DataStoreService dataStoreService;
  @Inject private VerificationServiceConfiguration verificationServiceConfiguration;

  @Override
  public boolean saveMetricData(final String accountId, final String appId, final String stateExecutionId,
      String delegateTaskId, List<NewRelicMetricDataRecord> metricData) {
    if (isEmpty(metricData)) {
      logger.info("For state {} received empty collection", stateExecutionId);
      return false;
    }
    metricData.forEach(metric -> {
      if (isNotEmpty(metric.getCvConfigId())) {
        metric.setValidUntil(Date.from(OffsetDateTime.now().plusMonths(1).toInstant()));
      }
    });
    if (!isStateValid(appId, stateExecutionId)) {
      logger.info("State is no longer active {}. Sending delegate abort request {}", stateExecutionId, delegateTaskId);
      return false;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("inserting " + metricData.size() + " pieces of new relic metrics data");
    }
    try {
      dataStoreService.save(NewRelicMetricDataRecord.class, metricData);
    } catch (Exception e) {
      logger.error("Exception writing data to storage", e);
    }

    if (verificationServiceConfiguration.getDataStorageMode().equals(DataStorageMode.GOOGLE_CLOUD_DATA_STORE)) {
      wingsPersistence.saveIgnoringDuplicateKeys(metricData);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("inserted " + metricData.size() + " NewRelicMetricDataRecord to persistence layer.");
    }
    return true;
  }

  @Override
  public void saveAnalysisRecords(final NewRelicMetricAnalysisRecord metricAnalysisRecord) {
    wingsPersistence.save(metricAnalysisRecord);
    logger.info(
        "inserted NewRelicMetricAnalysisRecord to persistence layer for workflowExecutionId: {} StateExecutionInstanceId: {}",
        metricAnalysisRecord.getWorkflowExecutionId(), metricAnalysisRecord.getStateExecutionId());
  }

  /**
   * Method to save ml analysed records to mongoDB
   *
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
  public boolean saveAnalysisRecordsML(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String groupName, Integer analysisMinute, String taskId, String baseLineExecutionId,
      String cvConfigId, MetricAnalysisRecord mlAnalysisResponse) {
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

    riskSummary.setTxnMetricRisk(risks.rowMap());
    riskSummary.setTxnMetricLongTermPattern(longTermPatterns.rowMap());
    riskSummary.setTxnMetricRiskData(riskData.rowMap());
    riskSummary.compressMaps();

    mlAnalysisResponse.setAggregatedRisk(aggregatedRisk);

    saveTimeSeriesMLScores(timeSeriesMLScores);
    bumpCollectionMinuteToProcess(appId, stateExecutionId, workflowExecutionId, groupName, analysisMinute);
    learningEngineService.markCompleted(taskId);

    mlAnalysisResponse.compressTransactions();

    if (isNotEmpty(mlAnalysisResponse.getAnomalies())) {
      TimeSeriesAnomaliesRecord anomaliesRecord = wingsPersistence.createQuery(TimeSeriesAnomaliesRecord.class)
                                                      .filter("appId", appId)
                                                      .filter("cvConfigId", cvConfigId)
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

      cumulativeSums.compressMetricSums();
      wingsPersistence.save(cumulativeSums);
    }
    wingsPersistence.save(mlAnalysisResponse);
    wingsPersistence.save(riskSummary);

    if (!isEmpty(mlAnalysisResponse.getOverallMetricScores())) {
      double finalOverallScore =
          mlAnalysisResponse.getOverallMetricScores().values().stream().mapToDouble(val -> val).average().orElse(0.0);
      if (!isEmpty(cvConfigId) && finalOverallScore >= MEDIUM_RISK_CUTOFF) {
        logger.warn("Risk detected for cvConfigId {}, stateType {}, finalOverallScore {}", cvConfigId, stateType,
            finalOverallScore);
      }
    }
    logger.info("inserted MetricAnalysisRecord to persistence layer for "
            + "stateType: {}, workflowExecutionId: {} StateExecutionInstanceId: {}",
        stateType, workflowExecutionId, stateExecutionId);
    return true;
  }

  @Override
  public List<TimeSeriesMLScores> getTimeSeriesMLScores(
      String appId, String workflowId, int analysisMinute, int limit) {
    List<String> workflowExecutionIds = getLastSuccessfulWorkflowExecutionIds(appId, workflowId, null);
    return wingsPersistence.createQuery(TimeSeriesMLScores.class)
        .filter("workflowId", workflowId)
        .filter("appId", appId)
        .filter("analysisMinute", analysisMinute)
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
  public List<NewRelicMetricDataRecord> getRecords(String appId, String stateExecutionId, String groupName,
      Set<String> nodes, int analysisMinute, int analysisStartMinute) {
    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("stateExecutionId", Operator.EQ, stateExecutionId)
              .addFilter("groupName", Operator.EQ, groupName)
              .addFilter("dataCollectionMinute", Operator.LT_EQ, analysisMinute)
              .addFilter("dataCollectionMinute", Operator.GE, analysisStartMinute)
              .build();

      addAppIdInRequestIfNecessary(pageRequest, appId);
      final PageResponse<NewRelicMetricDataRecord> results =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
      return results.stream()
          .filter(dataRecord
              -> nodes.contains(dataRecord.getHost()) && !ClusterLevel.H0.equals(dataRecord.getLevel())
                  && !ClusterLevel.HF.equals(dataRecord.getLevel()))
          .collect(Collectors.toList());
    }
    return wingsPersistence.createQuery(NewRelicMetricDataRecord.class, excludeCount)
        .filter("stateExecutionId", stateExecutionId)
        .filter("appId", appId)
        .filter("groupName", groupName)
        .field("host")
        .hasAnyOf(nodes)
        .field("level")
        .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
        .field("dataCollectionMinute")
        .lessThanOrEq(analysisMinute)
        .field("dataCollectionMinute")
        .greaterThanOrEq(analysisStartMinute)
        .asList();
  }

  @Override
  public List<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(
      String appId, String workflowExecutionId, String groupName, int analysisMinute, int analysisStartMinute) {
    if (isEmpty(workflowExecutionId)) {
      return Collections.emptyList();
    }
    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("workflowExecutionId", Operator.EQ, workflowExecutionId)
              .addFilter("groupName", Operator.EQ, groupName)
              .addFilter("dataCollectionMinute", Operator.LT_EQ, analysisMinute)
              .addFilter("dataCollectionMinute", Operator.GE, analysisStartMinute)
              .build();

      addAppIdInRequestIfNecessary(pageRequest, appId);
      final PageResponse<NewRelicMetricDataRecord> results =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
      return results.stream()
          .filter(dataRecord
              -> !ClusterLevel.H0.equals(dataRecord.getLevel()) && !ClusterLevel.HF.equals(dataRecord.getLevel()))
          .collect(Collectors.toList());
    }

    MorphiaIterator<NewRelicMetricDataRecord, NewRelicMetricDataRecord> iterator =
        wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
            .filter("workflowExecutionId", workflowExecutionId)
            .filter("appId", appId)
            .field("groupName")
            .in(asList(groupName, NewRelicMetricDataRecord.DEFAULT_GROUP_NAME))
            .field("level")
            .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
            .field("dataCollectionMinute")
            .lessThanOrEq(analysisMinute)
            .field("dataCollectionMinute")
            .greaterThanOrEq(analysisStartMinute)
            .fetch();
    List<NewRelicMetricDataRecord> records = new ArrayList<>();
    while (iterator.hasNext()) {
      records.add(iterator.next());
    }
    return records;
  }

  @Override
  public int getMaxControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName) {
    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("workflowExecutionId", Operator.EQ, workflowExecutionId)
              .build();
      addAppIdInRequestIfNecessary(pageRequest, appId);

      PageResponse<NewRelicMetricDataRecord> results =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
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
      Collections.sort(records, (o1, o2) -> o2.getDataCollectionMinute() - o1.getDataCollectionMinute());
      return records.get(0).getDataCollectionMinute();
    }

    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .filter("stateType", stateType)
                                                            .filter("appId", appId)
                                                            .filter("workflowId", workflowId)
                                                            .filter("workflowExecutionId", workflowExecutionId)
                                                            .filter("serviceId", serviceId)
                                                            .field("groupName")
                                                            .in(asList(groupName, DEFAULT_GROUP_NAME))
                                                            .field("level")
                                                            .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                            .order("-dataCollectionMinute")
                                                            .get();

    return newRelicMetricDataRecord == null ? -1 : newRelicMetricDataRecord.getDataCollectionMinute();
  }

  @Override
  public int getMinControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName) {
    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("workflowExecutionId", Operator.EQ, workflowExecutionId)
              .build();
      addAppIdInRequestIfNecessary(pageRequest, appId);

      PageResponse<NewRelicMetricDataRecord> results =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
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
      Collections.sort(records, Comparator.comparingInt(NewRelicMetricDataRecord::getDataCollectionMinute));
      return records.get(0).getDataCollectionMinute();
    }
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .filter("stateType", stateType)
                                                            .filter("appId", appId)
                                                            .filter("workflowId", workflowId)
                                                            .filter("workflowExecutionId", workflowExecutionId)
                                                            .filter("serviceId", serviceId)
                                                            .filter("groupName", groupName)
                                                            .field("level")
                                                            .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
                                                            .order("dataCollectionMinute")
                                                            .get();

    return newRelicMetricDataRecord == null ? -1 : newRelicMetricDataRecord.getDataCollectionMinute();
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId) {
    List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(appId, workflowId, serviceId);
    for (String successfulExecution : successfulExecutions) {
      if (isGoogleMetricReadEnabled(appId)) {
        PageRequest<NewRelicMetricDataRecord> pageRequest =
            aPageRequest()
                .withLimit(UNLIMITED)
                .addFilter("workflowExecutionId", Operator.EQ, successfulExecution)
                .build();
        addAppIdInRequestIfNecessary(pageRequest, appId);

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

      if (wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
              .filter("stateType", stateType)
              .filter("appId", appId)
              .filter("workflowId", workflowId)
              .filter("workflowExecutionId", successfulExecution)
              .filter("serviceId", serviceId)
              .field("level")
              .notIn(asList(ClusterLevel.H0, ClusterLevel.HF))
              .count(new CountOptions().limit(1))
          > 0) {
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
  public List<NewRelicMetricAnalysisRecord> getMetricsAnalysis(
      final String appId, final String stateExecutionId, final String workflowExecutionId) {
    List<NewRelicMetricAnalysisRecord> analysisRecords = new ArrayList<>();
    List<TimeSeriesMLAnalysisRecord> allAnalysisRecords =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("appId", appId)
            .filter("stateExecutionId", stateExecutionId)
            .filter("workflowExecutionId", workflowExecutionId)
            .order(Sort.descending(TimeSeriesMLAnalysisRecord.CREATED_AT_KEY))
            .asList();

    Map<String, TimeSeriesMLAnalysisRecord> groupVsAnalysisRecord = new HashMap<>();
    allAnalysisRecords.forEach(analysisRecord -> {
      analysisRecord.decompressTransactions();
      if (!groupVsAnalysisRecord.containsKey(analysisRecord.getGroupName())) {
        groupVsAnalysisRecord.put(analysisRecord.getGroupName(), analysisRecord);
      }
    });
    Collection<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords = groupVsAnalysisRecord.values();
    timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
      NewRelicMetricAnalysisRecord metricAnalysisRecord =
          NewRelicMetricAnalysisRecord.builder()
              .appId(timeSeriesMLAnalysisRecord.getAppId())
              .stateType(timeSeriesMLAnalysisRecord.getStateType())
              .analysisMinute(timeSeriesMLAnalysisRecord.getAnalysisMinute())
              .stateExecutionId(timeSeriesMLAnalysisRecord.getStateExecutionId())
              .workflowExecutionId(timeSeriesMLAnalysisRecord.getWorkflowExecutionId())
              .baseLineExecutionId(timeSeriesMLAnalysisRecord.getBaseLineExecutionId())
              .showTimeSeries(true)
              .groupName(timeSeriesMLAnalysisRecord.getGroupName())
              .message(timeSeriesMLAnalysisRecord.getMessage())
              .build();
      analysisRecords.add(metricAnalysisRecord);
      if (timeSeriesMLAnalysisRecord.getTransactions() != null) {
        List<NewRelicMetricAnalysis> metricAnalysisList = new ArrayList<>();
        for (TimeSeriesMLTxnSummary txnSummary : timeSeriesMLAnalysisRecord.getTransactions().values()) {
          List<NewRelicMetricAnalysisValue> metricsList = new ArrayList<>();
          RiskLevel globalRisk = RiskLevel.NA;
          for (TimeSeriesMLMetricSummary mlMetricSummary : txnSummary.getMetrics().values()) {
            RiskLevel riskLevel = getRiskLevel(mlMetricSummary.getMax_risk());

            if (riskLevel.compareTo(globalRisk) < 0) {
              globalRisk = riskLevel;
            }
            metricsList.add(NewRelicMetricAnalysisValue.builder()
                                .name(mlMetricSummary.getMetric_name())
                                .type(mlMetricSummary.getMetric_type())
                                .alertType(mlMetricSummary.getAlert_type())
                                .riskLevel(riskLevel)
                                .controlValue(mlMetricSummary.getControl_avg())
                                .testValue(mlMetricSummary.getTest_avg())
                                .build());
          }
          metricAnalysisList.add(NewRelicMetricAnalysis.builder()
                                     .metricName(txnSummary.getTxn_name())
                                     .tag(txnSummary.getTxn_tag())
                                     .metricValues(metricsList)
                                     .riskLevel(globalRisk)
                                     .build());
        }
        metricAnalysisRecord.setMetricAnalyses(metricAnalysisList);
      }
    });
    List<NewRelicMetricAnalysisRecord> allMetricAnalyisRecords =
        wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class)
            .filter("appId", appId)
            .filter("stateExecutionId", stateExecutionId)
            .filter("workflowExecutionId", workflowExecutionId)
            .order(Sort.descending(NewRelicMetricAnalysisRecord.CREATED_AT_KEY))
            .asList();

    Map<String, NewRelicMetricAnalysisRecord> groupVsMetricAnalysisRecord = new HashMap<>();
    allMetricAnalyisRecords.forEach(analysisRecord -> {
      if (!groupVsMetricAnalysisRecord.containsKey(analysisRecord.getGroupName())) {
        groupVsMetricAnalysisRecord.put(analysisRecord.getGroupName(), analysisRecord);
      }
    });
    analysisRecords.addAll(groupVsMetricAnalysisRecord.values());

    if (isEmpty(analysisRecords)) {
      analysisRecords.add(NewRelicMetricAnalysisRecord.builder()
                              .showTimeSeries(false)
                              .stateType(StateType.APP_DYNAMICS)
                              .riskLevel(RiskLevel.NA)
                              .message("No data available")
                              .build());
    }

    Map<String, TimeSeriesMlAnalysisGroupInfo> metricGroups = getMetricGroups(appId, stateExecutionId);
    analysisRecords.forEach(analysisRecord -> {
      TimeSeriesMlAnalysisGroupInfo mlAnalysisGroupInfo = metricGroups.get(analysisRecord.getGroupName());
      analysisRecord.setDependencyPath(mlAnalysisGroupInfo == null ? null : mlAnalysisGroupInfo.getDependencyPath());
      analysisRecord.setMlAnalysisType(
          mlAnalysisGroupInfo == null ? TimeSeriesMlAnalysisType.COMPARATIVE : mlAnalysisGroupInfo.getMlAnalysisType());
      if (analysisRecord.getMetricAnalyses() != null) {
        int highRisk = 0;
        int mediumRisk = 0;
        for (NewRelicMetricAnalysis metricAnalysis : analysisRecord.getMetricAnalyses()) {
          final RiskLevel riskLevel = metricAnalysis.getRiskLevel();
          switch (riskLevel) {
            case HIGH:
              highRisk++;
              break;
            case MEDIUM:
              mediumRisk++;
              break;
            case NA:
              noop();
              break;
            case LOW:
              noop();
              break;
            default:
              unhandled(riskLevel);
          }
        }

        if (highRisk == 0 && mediumRisk == 0) {
          analysisRecord.setMessage("No problems found");
        } else {
          StringBuffer message = new StringBuffer(20);
          if (highRisk > 0) {
            message.append(highRisk + " high risk " + (highRisk > 1 ? "transactions" : "transaction") + " found. ");
          }

          if (mediumRisk > 0) {
            message.append(
                mediumRisk + " medium risk " + (mediumRisk > 1 ? "transactions" : "transaction") + " found.");
          }

          analysisRecord.setMessage(message.toString());
        }

        if (highRisk > 0) {
          analysisRecord.setRiskLevel(RiskLevel.HIGH);
        } else if (mediumRisk > 0) {
          analysisRecord.setRiskLevel(RiskLevel.MEDIUM);
        } else {
          analysisRecord.setRiskLevel(RiskLevel.LOW);
        }

        Collections.sort(analysisRecord.getMetricAnalyses());
      } else {
        analysisRecord.setRiskLevel(RiskLevel.NA);
      }

      if (analysisRecord.getStateType() == StateType.DYNA_TRACE && !isEmpty(analysisRecord.getMetricAnalyses())) {
        for (NewRelicMetricAnalysis analysis : analysisRecord.getMetricAnalyses()) {
          String metricName = analysis.getMetricName();
          String[] split = metricName.split(":");
          if (split == null || split.length == 1) {
            analysis.setDisplayName(metricName);
            analysis.setFullMetricName(metricName);
            continue;
          }
          String btName = split[0];
          String fullBTName = btName + " (" + metricName.substring(btName.length() + 1) + ")";
          analysis.setDisplayName(btName);
          analysis.setFullMetricName(fullBTName);
        }
      }
    });
    Collections.sort(analysisRecords);
    return analysisRecords;
  }

  @Override
  public boolean isStateValid(String appId, String stateExecutionId) {
    if (stateExecutionId.contains(CV_24x7_STATE_EXECUTION)) {
      return true;
    }
    return managerClientHelper.callManagerWithRetry(managerClient.isStateValid(appId, stateExecutionId)).getResource();
  }

  @Override
  public NewRelicMetricDataRecord getLastHeartBeat(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName) {
    logger.info(
        "Querying for getLastHeartBeat. Params are: stateType {}, appId {}, stateExecutionId: {}, workflowExecutionId: {} serviceId {}, groupName: {} ",
        stateType, appId, stateExecutionId, workflowExecutionId, serviceId, groupName);

    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("stateExecutionId", Operator.EQ, stateExecutionId)
              .addFilter("groupName", Operator.EQ, groupName)
              .addOrder("dataCollectionMinute", OrderType.DESC)
              .build();

      addAppIdInRequestIfNecessary(pageRequest, appId);
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

    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .filter("stateType", stateType)
                                                            .filter("appId", appId)
                                                            .filter("workflowExecutionId", workflowExecutionId)
                                                            .filter("stateExecutionId", stateExecutionId)
                                                            .filter("serviceId", serviceId)
                                                            .filter("groupName", groupName)
                                                            .field("level")
                                                            .in(Lists.newArrayList(ClusterLevel.HF))
                                                            .order("-dataCollectionMinute")
                                                            .get();

    if (newRelicMetricDataRecord == null) {
      logger.info(
          "No heartbeat record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}",
          ClusterLevel.HF, stateExecutionId, workflowExecutionId, serviceId);
      return null;
    }
    logger.info("Returning the record {} from getLastHeartBeat", newRelicMetricDataRecord);
    return newRelicMetricDataRecord;
  }

  @Override
  public NewRelicMetricDataRecord getAnalysisMinute(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName) {
    logger.info(
        "Querying for getLastHeartBeat. Params are: stateType {}, appId {}, stateExecutionId: {}, workflowExecutionId: {} serviceId {}, groupName: {} ",
        stateType, appId, stateExecutionId, workflowExecutionId, serviceId, groupName);
    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("stateExecutionId", Operator.EQ, stateExecutionId)
              .addFilter("groupName", Operator.EQ, groupName)
              .addOrder("dataCollectionMinute", OrderType.DESC)
              .build();

      addAppIdInRequestIfNecessary(pageRequest, appId);

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
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .filter("stateType", stateType)
                                                            .filter("appId", appId)
                                                            .filter("workflowExecutionId", workflowExecutionId)
                                                            .filter("stateExecutionId", stateExecutionId)
                                                            .filter("serviceId", serviceId)
                                                            .filter("groupName", groupName)
                                                            .field("level")
                                                            .in(Lists.newArrayList(ClusterLevel.H0))
                                                            .order("-dataCollectionMinute")
                                                            .get();

    if (newRelicMetricDataRecord == null) {
      logger.info(
          "No metric record with heartbeat level {} found for stateExecutionId: {}, workflowExecutionId: {}, serviceId: {}.",
          ClusterLevel.H0, stateExecutionId, workflowExecutionId, serviceId);
      return null;
    }
    logger.info("Returning the record: {} from getAnalysisMinute", newRelicMetricDataRecord);
    return newRelicMetricDataRecord;
  }

  @Override
  public void bumpCollectionMinuteToProcess(
      String appId, String stateExecutionId, String workflowExecutionId, String groupName, int analysisMinute) {
    logger.info(
        "bumpCollectionMinuteToProcess. Going to update the record for stateExecutionId {} and dataCollectionMinute {}",
        stateExecutionId, analysisMinute);
    if (isEmpty(stateExecutionId)) {
      return;
    }
    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("stateExecutionId", Operator.EQ, stateExecutionId)
              .addFilter("groupName", Operator.EQ, groupName)
              .addFilter("level", Operator.EQ, ClusterLevel.H0)
              .addFilter("dataCollectionMinute", Operator.LT_EQ, analysisMinute)
              .addOrder("dataCollectionMinute", OrderType.DESC)
              .build();
      addAppIdInRequestIfNecessary(pageRequest, appId);
      final PageResponse<NewRelicMetricDataRecord> dataRecords =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);

      dataRecords.forEach(dataRecord -> dataRecord.setLevel(ClusterLevel.HF));
      dataStoreService.save(NewRelicMetricDataRecord.class, dataRecords);
    }

    Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                .filter("appId", appId)
                                                .filter("workflowExecutionId", workflowExecutionId)
                                                .filter("stateExecutionId", stateExecutionId)
                                                .filter("groupName", groupName)
                                                .filter("level", ClusterLevel.H0)
                                                .field("dataCollectionMinute")
                                                .lessThanOrEq(analysisMinute);

    UpdateResults updateResults = wingsPersistence.update(
        query, wingsPersistence.createUpdateOperations(NewRelicMetricDataRecord.class).set("level", ClusterLevel.HF));
    logger.info("bumpCollectionMinuteToProcess updated results size {}, for stateExecutionId {} ",
        updateResults.getUpdatedCount(), stateExecutionId);
  }

  @Override
  public Map<String, TimeSeriesMetricDefinition> getMetricTemplates(
      String appId, StateType stateType, String stateExecutionId, String cvConfigId) {
    TimeSeriesMetricTemplates newRelicMetricTemplates = wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
                                                            .field("appId")
                                                            .equal(appId)
                                                            .field("stateType")
                                                            .equal(stateType)
                                                            .field("stateExecutionId")
                                                            .equal(stateExecutionId)
                                                            .field("cvConfigId")
                                                            .equal(cvConfigId)
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
  public int getMaxCVCollectionMinute(String appId, String cvConfigId) {
    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest = aPageRequest()
                                                              .withLimit("1")
                                                              .addFilter("cvConfigId", Operator.EQ, cvConfigId)
                                                              .addOrder("dataCollectionMinute", OrderType.DESC)
                                                              .build();
      addAppIdInRequestIfNecessary(pageRequest, appId);

      final PageResponse<NewRelicMetricDataRecord> results =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
      if (isEmpty(results)) {
        return -1;
      }
      return results.get(0).getDataCollectionMinute();
    }
    NewRelicMetricDataRecord newRelicMetricDataRecord = wingsPersistence.createQuery(NewRelicMetricDataRecord.class)
                                                            .filter("appId", appId)
                                                            .filter("cvConfigId", cvConfigId)
                                                            .order("-dataCollectionMinute")
                                                            .get();

    return newRelicMetricDataRecord == null ? -1 : newRelicMetricDataRecord.getDataCollectionMinute();
  }

  @Override
  public long getLastCVAnalysisMinute(String appId, String cvConfigId) {
    TimeSeriesMLAnalysisRecord newRelicMetricAnalysisRecord =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("appId", appId)
            .filter("cvConfigId", cvConfigId)
            .order("-analysisMinute")
            .get();
    return newRelicMetricAnalysisRecord == null ? -1 : newRelicMetricAnalysisRecord.getAnalysisMinute();
  }

  @Override
  public List<NewRelicMetricDataRecord> getMetricRecords(StateType stateType, String appId, String serviceId,
      String cvConfigId, int analysisStartMinute, int analysisEndMinute) {
    if (isGoogleMetricReadEnabled(appId)) {
      PageRequest<NewRelicMetricDataRecord> pageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter("cvConfigId", Operator.EQ, cvConfigId)
              .addFilter("dataCollectionMinute", Operator.GE, analysisStartMinute)
              .addFilter("dataCollectionMinute", Operator.LT_EQ, analysisEndMinute)
              .build();
      addAppIdInRequestIfNecessary(pageRequest, appId);

      final PageResponse<NewRelicMetricDataRecord> results =
          dataStoreService.list(NewRelicMetricDataRecord.class, pageRequest);
      return results.stream()
          .filter(dataRecord -> !dataRecord.getName().equals(HARNESS_HEARTBEAT_METRIC_NAME))
          .collect(Collectors.toList());
    }
    return wingsPersistence.createQuery(NewRelicMetricDataRecord.class, excludeCount)
        .filter("stateType", stateType)
        .filter("appId", appId)
        .filter("serviceId", serviceId)
        .filter("cvConfigId", cvConfigId)
        .field("name")
        .notEqual(HARNESS_HEARTBEAT_METRIC_NAME)
        .field("dataCollectionMinute")
        .greaterThanOrEq(analysisStartMinute)
        .field("dataCollectionMinute")
        .lessThanOrEq(analysisEndMinute)
        .asList();
  }

  @Override
  public TimeSeriesMLAnalysisRecord getPreviousAnalysis(String appId, String cvConfigId, long dataCollectionMin) {
    final TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("appId", appId)
            .filter("cvConfigId", cvConfigId)
            .filter("analysisMinute", dataCollectionMin)
            .get();
    if (timeSeriesMLAnalysisRecord != null) {
      timeSeriesMLAnalysisRecord.decompressTransactions();
    }
    return timeSeriesMLAnalysisRecord;
  }

  @Override
  public List<TimeSeriesMLAnalysisRecord> getHistoricalAnalysis(
      String accountId, String appId, String serviceId, String cvConfigId, long analysisMin) {
    List<Long> historicalAnalysisTimes = new ArrayList<>();

    for (int i = 1; i <= VerificationConstants.CANARY_DAYS_TO_COLLECT; i++) {
      historicalAnalysisTimes.add(analysisMin - i * TimeUnit.DAYS.toMinutes(7));
    }

    if (historicalAnalysisTimes.size() == 0) {
      return new ArrayList<>();
    }

    final List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
            .filter("appId", appId)
            .filter("cvConfigId", cvConfigId)
            .field("analysisMinute")
            .in(historicalAnalysisTimes)
            .asList();
    if (timeSeriesMLAnalysisRecords != null) {
      timeSeriesMLAnalysisRecords.forEach(
          timeSeriesMLAnalysisRecord -> timeSeriesMLAnalysisRecord.decompressTransactions());
    }
    return timeSeriesMLAnalysisRecords;
  }

  @Override
  public TimeSeriesAnomaliesRecord getPreviousAnomalies(
      String appId, String cvConfigId, Map<String, List<String>> metrics) {
    TimeSeriesAnomaliesRecord timeSeriesAnomaliesRecord = wingsPersistence.createQuery(TimeSeriesAnomaliesRecord.class)
                                                              .filter("appId", appId)
                                                              .filter("cvConfigId", cvConfigId)
                                                              .get();

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
  public List<TimeSeriesCumulativeSums> getCumulativeSumsForRange(
      String appId, String cvConfigId, int startMinute, int endMinute) {
    if (isNotEmpty(appId) && isNotEmpty(cvConfigId) && startMinute <= endMinute) {
      List<TimeSeriesCumulativeSums> cumulativeSums = wingsPersistence.createQuery(TimeSeriesCumulativeSums.class)
                                                          .filter("appId", appId)
                                                          .filter("cvConfigId", cvConfigId)
                                                          .field("analysisMinute")
                                                          .greaterThanOrEq(startMinute)
                                                          .field("analysisMinute")
                                                          .lessThanOrEq(endMinute)
                                                          .asList();

      logger.info(
          "Returning a list of size {} from getCumulativeSumsForRange for appId {}, cvConfigId {} and start {} and end {}",
          cumulativeSums.size(), appId, cvConfigId, startMinute, endMinute);
      cumulativeSums.forEach(metricSum -> metricSum.decompressMetricSums());
      return cumulativeSums;
    } else {
      final String errorMsg = "AppId or CVConfigId is null in getCumulativeSumsForRange";
      logger.error(errorMsg);
      throw new WingsException(errorMsg);
    }
  }

  private void addAppIdInRequestIfNecessary(PageRequest pageRequest, String appId) {
    if (dataStoreService instanceof MongoDataStoreServiceImpl) {
      pageRequest.addFilter("appId", Operator.EQ, appId);
    }
  }

  private boolean isGoogleMetricReadEnabled(String appId) {
    return managerClientHelper
        .callManagerWithRetry(managerClient.isFeatureEnabled(
            FeatureName.METRIC_READ_GOOGLE, wingsPersistence.get(Application.class, appId).getAccountId()))
        .getResource();
  }
}
