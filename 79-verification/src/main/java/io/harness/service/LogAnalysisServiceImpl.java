package io.harness.service;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.LT_EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeCount;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.service.intfc.analysis.ClusterLevel.H0;
import static software.wings.service.intfc.analysis.ClusterLevel.H1;
import static software.wings.service.intfc.analysis.ClusterLevel.H2;
import static software.wings.service.intfc.analysis.ClusterLevel.HF;
import static software.wings.service.intfc.analysis.ClusterLevel.L0;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.eraro.ErrorCode;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.exception.WingsException;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.persistence.HIterator;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.InstanceElement;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLClusterSummary;
import software.wings.service.impl.analysis.LogMLExpAnalysisInfo;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.verification.log.LogsCVConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Created by Praveen
 */
public class LogAnalysisServiceImpl implements LogAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);
  private static final double HIGH_RISK_THRESHOLD = 50;
  private static final double MEDIUM_RISK_THRESHOLD = 25;

  private final Random random = new Random();

  @Inject protected WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;
  @Inject private LearningEngineService learningEngineService;
  @Inject private VerificationManagerClientHelper managerClientHelper;
  @Inject private VerificationManagerClient managerClient;
  @Inject private HarnessMetricRegistry metricRegistry;
  @Inject private UsageMetricsHelper usageMetricsHelper;
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void bumpClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, long logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel) {
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class)
                                     .filter("stateType", stateType)
                                     .filter("stateExecutionId", stateExecutionId)
                                     .filter("appId", appId)
                                     .filter("query", searchQuery)
                                     .filter("logCollectionMinute", logCollectionMinute)
                                     .filter("clusterLevel", fromLevel);

    if (isNotEmpty(host)) {
      query = query.field("host").in(host);
    }
    try {
      UpdateResults results = wingsPersistence.update(
          query, wingsPersistence.createUpdateOperations(LogDataRecord.class).set("clusterLevel", toLevel));
      logger.info("for {} bumped records {}", stateExecutionId, results.getUpdatedCount());
    } catch (DuplicateKeyException e) {
      logger.warn(
          "duplicate update operation for state: {}, stateExecutionId: {}, searchQuery: {}, hosts: {}, logCollectionMinute: {}, from: {}, to: {}",
          stateType, stateExecutionId, searchQuery, host, logCollectionMinute, fromLevel, toLevel);
    }
  }

  @Override
  public void deleteClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, long logCollectionMinute, ClusterLevel... clusterLevels) {
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class)
                                     .filter("stateType", stateType)
                                     .filter("stateExecutionId", stateExecutionId)
                                     .filter("appId", appId)
                                     .filter("query", searchQuery)
                                     .filter("logCollectionMinute", logCollectionMinute)
                                     .field("clusterLevel")
                                     .in(asList(clusterLevels));
    if (isNotEmpty(host)) {
      query = query.field("host").in(host);
    }
    wingsPersistence.delete(query);
  }

  private void deleteClusterLevel(String appId, String cvConfigId, String host, int logCollectionMinute,
      ClusterLevel fromLevel, ClusterLevel toLevel) {
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                     .filter("cvConfigId", cvConfigId)
                                     .filter("clusterLevel", fromLevel);

    if (ClusterLevel.L2.equals(toLevel)) {
      query = query.field("logCollectionMinute").lessThanOrEq(logCollectionMinute);
    } else {
      query = query.filter("logCollectionMinute", logCollectionMinute);
    }

    if (isNotEmpty(host)) {
      query = query.filter("host", host);
    }
    wingsPersistence.delete(query);
    logger.info("Deleted clustered data for cvConfigId: {}, minute {}, fromLevel {}, toLevel {}", cvConfigId,
        logCollectionMinute, fromLevel, toLevel);
    try {
      query = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                  .filter("cvConfigId", cvConfigId)
                  .filter("clusterLevel", ClusterLevel.getHeartBeatLevel(fromLevel));

      if (ClusterLevel.L2.equals(toLevel)) {
        query = query.field("logCollectionMinute").lessThanOrEq(logCollectionMinute);
      } else {
        query = query.filter("logCollectionMinute", logCollectionMinute);
      }

      if (isNotEmpty(host)) {
        query = query.filter("host", host);
      }
      UpdateResults updatedResults = wingsPersistence.update(query,
          wingsPersistence.createUpdateOperations(LogDataRecord.class)
              .set("clusterLevel", ClusterLevel.getHeartBeatLevel(toLevel)));
      if (updatedResults.getUpdatedCount() == 0 && host.equals(DUMMY_HOST_NAME)) {
        logger.error("did not update heartbeat from {} to {}  for min {}", fromLevel, toLevel, logCollectionMinute);
      }
    } catch (DuplicateKeyException e) {
      logger.error("for {} for hosts {} for min {} level is already updated to {}", cvConfigId, host,
          logCollectionMinute, toLevel);
    }
  }

  @Override
  public Boolean saveLogData(StateType stateType, String accountId, String appId, String cvConfigId,
      String stateExecutionId, String workflowId, String workflowExecutionId, String serviceId,
      ClusterLevel clusterLevel, String delegateTaskId, List<LogElement> logData) {
    try {
      if (isEmpty(cvConfigId) && !isStateValid(appId, stateExecutionId)) {
        logger.warn(
            "State is no longer active " + stateExecutionId + ". Sending delegate abort request " + delegateTaskId);
        return false;
      }
      logger.info("inserting " + logData.size() + " pieces of log data");

      if (logData.isEmpty()) {
        return true;
      }

      boolean hasHeartBeat =
          logData.stream().anyMatch((LogElement data) -> Integer.parseInt(data.getClusterLabel()) < 0);

      /*
       * LOGZ, ELK, SUMO report cluster level L0. This is then clustered twice
       * in python and reported here as L1 and L2. Only L0 will have the heartbeat
       */
      if (clusterLevel == ClusterLevel.L0 && !hasHeartBeat) {
        logger.error("Delegate reporting log records without a "
            + "heartbeat for state " + stateType + " : id " + stateExecutionId);
        return false;
      }

      /*
       * The only time we see data for Splunk is from the delegate at level L2. There is no
       * additional clustering for Splunk
       */
      if (stateType == StateType.SPLUNKV2 && !hasHeartBeat) {
        logger.error("Delegate reporting log records without a "
            + "heartbeat for state " + stateType + " : id " + stateExecutionId);

        return false;
      }

      List<LogDataRecord> logDataRecords =
          LogDataRecord.generateDataRecords(stateType, appId, cvConfigId, stateExecutionId, workflowId,
              workflowExecutionId, serviceId, clusterLevel, ClusterLevel.getHeartBeatLevel(clusterLevel), logData);

      if (!validate24X7LogData(appId, cvConfigId, logDataRecords)) {
        return false;
      }
      wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);

      if (dataStoreService instanceof GoogleDataStoreServiceImpl && clusterLevel == ClusterLevel.L2) {
        dataStoreService.save(LogDataRecord.class, logDataRecords, true);
      }

      // bump the level for clustered data
      long logCollectionMinute = logData.get(0).getLogCollectionMinute();
      String query = logData.get(0).getQuery();
      switch (clusterLevel) {
        case L0:
          break;
        case L1:
          String node = logData.get(0).getHost();
          bumpClusterLevel(stateType, stateExecutionId, appId, query, Collections.singleton(node), logCollectionMinute,
              ClusterLevel.getHeartBeatLevel(ClusterLevel.L0), ClusterLevel.getHeartBeatLevel(ClusterLevel.L1));
          deleteClusterLevel(stateType, stateExecutionId, appId, query, Collections.singleton(node),
              logCollectionMinute, ClusterLevel.L0);
          learningEngineService.markCompleted(
              workflowExecutionId, stateExecutionId, logCollectionMinute, MLAnalysisType.LOG_CLUSTER, ClusterLevel.L1);
          break;
        case L2:
          bumpClusterLevel(stateType, stateExecutionId, appId, query, emptySet(), logCollectionMinute,
              ClusterLevel.getHeartBeatLevel(ClusterLevel.L1), ClusterLevel.getHeartBeatLevel(ClusterLevel.L2));
          deleteClusterLevel(
              stateType, stateExecutionId, appId, query, emptySet(), logCollectionMinute, ClusterLevel.L1);
          learningEngineService.markCompleted(
              workflowExecutionId, stateExecutionId, logCollectionMinute, MLAnalysisType.LOG_CLUSTER, ClusterLevel.L2);
          break;
        default:
          throw new WingsException("Bad cluster level {} " + clusterLevel.name());
      }
      return true;
    } catch (Exception ex) {
      logger.error("Save log data failed " + ex);
      return false;
    }
  }

  private boolean validate24X7LogData(String appId, String cvConfigId, List<LogDataRecord> logDataRecords) {
    if (isEmpty(cvConfigId)) {
      return true;
    }
    Set<Long> clusteredMinutes = new HashSet<>();
    logDataRecords.stream()
        .filter(logDataRecord -> logDataRecord.getClusterLevel().equals(H0))
        .forEach(logDataRecord -> {
          if (isNotEmpty(
                  getHostsForMinute(appId, cvConfigId, logDataRecord.getLogCollectionMinute(), H0, H1, H2, HF))) {
            clusteredMinutes.add(logDataRecord.getLogCollectionMinute());
          }
        });
    if (clusteredMinutes.isEmpty()) {
      return true;
    }

    logger.error("for {} got logs for minutes {} which are already clustered", cvConfigId, clusteredMinutes);
    return false;
  }

  @Override
  public boolean saveClusteredLogData(String appId, String cvConfigId, ClusterLevel clusterLevel,
      int logCollectionMinute, String host, List<LogElement> logData) {
    final LogsCVConfiguration logsCVConfiguration = wingsPersistence.get(LogsCVConfiguration.class, cvConfigId);
    if (logsCVConfiguration == null) {
      logger.info("No configuration found for {} in app {}. It may have been deleted.", cvConfigId, appId);
      return false;
    }
    if (isNotEmpty(logData)) {
      List<LogDataRecord> logDataRecords =
          LogDataRecord.generateDataRecords(logsCVConfiguration.getStateType(), appId, cvConfigId, null, null, null,
              logsCVConfiguration.getServiceId(), clusterLevel, ClusterLevel.getHeartBeatLevel(clusterLevel), logData);
      wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);
      logger.info("Saved clustered data for cvConfig: {}, minute {}, toLevel {}", cvConfigId, logCollectionMinute,
          clusterLevel);

      if (dataStoreService instanceof GoogleDataStoreServiceImpl && clusterLevel.equals(ClusterLevel.L2)) {
        dataStoreService.save(LogDataRecord.class, logDataRecords, true);
        logger.info("Saved L2 clustered data to GoogleDatStore for cvConfig: {}, minute {}, toLevel {}", cvConfigId,
            logCollectionMinute, clusterLevel);
      }
    }

    switch (clusterLevel) {
      case L0:
        break;
      case L1:
        deleteClusterLevel(appId, cvConfigId, host, logCollectionMinute, ClusterLevel.L0, ClusterLevel.L1);
        if (isEmpty(getHostsForMinute(appId, cvConfigId, logCollectionMinute, L0))) {
          try {
            learningEngineService.markCompleted(null, "LOGS_CLUSTER_L1_" + cvConfigId + "_" + logCollectionMinute,
                logCollectionMinute, MLAnalysisType.LOG_CLUSTER, ClusterLevel.L1);
          } catch (DuplicateKeyException e) {
            logger.info(
                "for {} task for L1 clustering min {} has already marked completed", cvConfigId, logCollectionMinute);
          }
        }
        break;
      case L2:
        deleteClusterLevel(appId, cvConfigId, null, logCollectionMinute, ClusterLevel.L1, ClusterLevel.L2);
        learningEngineService.markCompleted(null, "LOGS_CLUSTER_L2_" + cvConfigId + "_" + logCollectionMinute,
            logCollectionMinute, MLAnalysisType.LOG_CLUSTER, ClusterLevel.L2);
        break;
      default:
        throw new WingsException("Bad cluster level {} " + clusterLevel.name());
    }
    return true;
  }

  @Override
  public Set<LogDataRecord> getLogData(LogRequest logRequest, boolean compareCurrent, String workflowExecutionId,
      ClusterLevel clusterLevel, StateType stateType, String accountId) {
    Query<LogDataRecord> recordQuery;

    if (compareCurrent) {
      recordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                        .filter("stateType", stateType)
                        .filter("stateExecutionId", logRequest.getStateExecutionId())
                        .filter("workflowExecutionId", workflowExecutionId)
                        .filter("appId", logRequest.getApplicationId())
                        .filter("query", logRequest.getQuery())
                        .filter("serviceId", logRequest.getServiceId())
                        .filter("clusterLevel", clusterLevel)
                        .filter("logCollectionMinute", logRequest.getLogCollectionMinute())
                        .field("host")
                        .hasAnyOf(logRequest.getNodes());

    } else {
      long timeDelta = 0;

      if (stateType.equals(StateType.SUMO)) {
        LogDataRecord logDataRecord = wingsPersistence.createQuery(LogDataRecord.class)
                                          .project("logCollectionMinute", true)
                                          .filter("stateType", stateType)
                                          .filter("stateExecutionId", logRequest.getStateExecutionId())
                                          .filter("appId", logRequest.getApplicationId())
                                          .filter("query", logRequest.getQuery())
                                          .filter("serviceId", logRequest.getServiceId())
                                          .filter("clusterLevel", clusterLevel)
                                          .order(Sort.ascending("logCollectionMinute"))
                                          .get();

        if (logDataRecord != null) {
          timeDelta = logRequest.getLogCollectionMinute() - logDataRecord.getLogCollectionMinute();
        }

        logDataRecord = wingsPersistence.createQuery(LogDataRecord.class)
                            .project("logCollectionMinute", true)
                            .filter("stateType", stateType)
                            .filter("workflowExecutionId", workflowExecutionId)
                            .filter("appId", logRequest.getApplicationId())
                            .filter("query", logRequest.getQuery())
                            .filter("serviceId", logRequest.getServiceId())
                            .filter("clusterLevel", clusterLevel)
                            .order(Sort.ascending("logCollectionMinute"))
                            .get();

        if (logDataRecord != null) {
          logRequest.setLogCollectionMinute(timeDelta + logDataRecord.getLogCollectionMinute());
        }
      }

      recordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                        .filter("stateType", stateType)
                        .filter("workflowExecutionId", workflowExecutionId)
                        .filter("appId", logRequest.getApplicationId())
                        .filter("query", logRequest.getQuery())
                        .filter("serviceId", logRequest.getServiceId())
                        .filter("clusterLevel", clusterLevel)
                        .filter("logCollectionMinute", logRequest.getLogCollectionMinute());
    }

    Set<LogDataRecord> rv = new HashSet<>();
    try (HIterator<LogDataRecord> records = new HIterator<>(recordQuery.fetch())) {
      while (records.hasNext()) {
        rv.add(records.next());
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("returning " + rv.size() + " records for request: " + logRequest);
    }
    return rv;
  }

  @Override
  public Set<LogDataRecord> getLogData(String appId, String cvConfigId, ClusterLevel clusterLevel,
      int logCollectionMinute, int startMinute, int endMinute, LogRequest logRequest) {
    Preconditions.checkState(
        logCollectionMinute > 0 ? startMinute <= 0 && endMinute <= 0 : startMinute >= 0 && endMinute >= 0,
        "both logCollectionMinute and start end minute set");
    Preconditions.checkState(!L0.equals(clusterLevel) || (L0.equals(clusterLevel) && isNotEmpty(logRequest.getNodes())),
        "for L0 -> L1 clustering nodes can not be empty, level: " + clusterLevel + " logRequest: " + logRequest);
    Set<LogDataRecord> logDataRecords = new HashSet<>();
    int previousOffset = 0;
    PageRequest<LogDataRecord> pageRequest = aPageRequest()
                                                 .withOffset(String.valueOf(previousOffset))
                                                 .withLimit("1000")
                                                 .addFilter("appId", Operator.EQ, appId)
                                                 .addFilter("cvConfigId", Operator.EQ, cvConfigId)
                                                 .addFilter("clusterLevel", Operator.EQ, clusterLevel)
                                                 .build();

    if (isNotEmpty(logRequest.getNodes())) {
      pageRequest.addFilter("host", Operator.IN, logRequest.getNodes().toArray());
    }

    if (logCollectionMinute > 0) {
      pageRequest.addFilter("logCollectionMinute", Operator.EQ, logCollectionMinute);
    } else {
      pageRequest.addFilter("logCollectionMinute", Operator.GE, startMinute);
      pageRequest.addFilter("logCollectionMinute", LT_EQ, endMinute);
    }

    PageResponse<LogDataRecord> response = wingsPersistence.query(LogDataRecord.class, pageRequest, excludeCount);
    while (!response.isEmpty()) {
      logDataRecords.addAll(response.getResponse());

      previousOffset += response.size();
      pageRequest.setOffset(String.valueOf(previousOffset));
      response = wingsPersistence.query(LogDataRecord.class, pageRequest, excludeCount);
    }
    return logDataRecords;
  }

  private boolean deleteFeedbackHelper(String feedbackId) {
    dataStoreService.delete(LogMLFeedbackRecord.class, feedbackId);

    return true;
  }

  @Override
  public boolean deleteFeedback(String feedbackId) {
    if (isEmpty(feedbackId)) {
      throw new WingsException("empty or null feedback id set ");
    }

    return deleteFeedbackHelper(feedbackId);
  }

  @Override
  public List<LogMLFeedbackRecord> getMLFeedback(
      String appId, String serviceId, String workflowId, String workflowExecutionId) {
    PageRequest<LogMLFeedbackRecord> feedbackRecordPageRequest =
        PageRequestBuilder.aPageRequest().withLimit(UNLIMITED).build();

    feedbackRecordPageRequest.addFilter("serviceId", Operator.EQ, serviceId);
    feedbackRecordPageRequest.addFilter("workflowId", Operator.EQ, workflowId);
    feedbackRecordPageRequest.addFilter("workflowExecutionId", Operator.EQ, workflowExecutionId);

    List<LogMLFeedbackRecord> records = dataStoreService.list(LogMLFeedbackRecord.class, feedbackRecordPageRequest);

    PageRequest<LogMLFeedbackRecord> feedbackRecordPageRequestServiceOnly =
        PageRequestBuilder.aPageRequest().withLimit(UNLIMITED).addFilter("serviceId", Operator.EQ, serviceId).build();

    List<LogMLFeedbackRecord> recordsServiceOnlyFilter =
        dataStoreService.list(LogMLFeedbackRecord.class, feedbackRecordPageRequestServiceOnly);

    Set<LogMLFeedbackRecord> recordSet = new HashSet<>();

    records.forEach(record -> recordSet.add(record));
    recordsServiceOnlyFilter.forEach(record -> recordSet.add(record));

    return new ArrayList<>(recordSet);
  }

  @Override
  public boolean isLogDataCollected(
      String appId, String stateExecutionId, String query, long logCollectionMinute, StateType stateType) {
    Query<LogDataRecord> splunkLogDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                                        .filter("stateType", stateType)
                                                        .filter("stateExecutionId", stateExecutionId)
                                                        .filter("appId", appId)
                                                        .filter("query", query)
                                                        .filter("logCollectionMinute", logCollectionMinute);
    return !splunkLogDataRecordQuery.asList().isEmpty();
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithLogs(
      StateType stateType, String appId, String serviceId, String workflowId, String query) {
    // TODO should we limit the number of executions to search in ??
    List<String> successfulExecutions = new ArrayList<>();
    List<ContinuousVerificationExecutionMetaData> cvList =
        wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class, excludeAuthority)
            .filter("applicationId", appId)
            .filter("stateType", stateType)
            .filter("workflowId", workflowId)
            .filter("executionStatus", ExecutionStatus.SUCCESS)
            .order("-workflowStartTs")
            .asList();
    cvList.forEach(cvMetadata -> successfulExecutions.add(cvMetadata.getWorkflowExecutionId()));
    for (String successfulExecution : successfulExecutions) {
      if (wingsPersistence.createQuery(LogDataRecord.class)
              .filter("appId", appId)
              .filter("workflowExecutionId", successfulExecution)
              .filter("clusterLevel", ClusterLevel.L2)
              .filter("query", query)
              .count(new CountOptions().limit(1))
          > 0) {
        return successfulExecution;
      }
    }
    if (isNotEmpty(successfulExecutions)) {
      return cvList.get(0).getWorkflowExecutionId();
    }
    logger.warn("Could not get a successful workflow to find control nodes");
    return null;
  }

  @Override
  public boolean saveLogAnalysisRecords(
      LogMLAnalysisRecord mlAnalysisResponse, StateType stateType, Optional<String> taskId) {
    mlAnalysisResponse.setStateType(stateType);
    boolean isAnalysisEmpty =
        isEmpty(mlAnalysisResponse.getControl_events()) && isEmpty(mlAnalysisResponse.getTest_events());
    mlAnalysisResponse.compressLogAnalysisRecord();

    if (mlAnalysisResponse.getLogCollectionMinute() == -1 || !isAnalysisEmpty) {
      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(mlAnalysisResponse));
      logger.info("inserted ml LogMLAnalysisRecord to persistence layer for stateExecutionInstanceId: {}",
          mlAnalysisResponse.getStateExecutionId());
    }
    bumpClusterLevel(stateType, mlAnalysisResponse.getStateExecutionId(), mlAnalysisResponse.getAppId(),
        mlAnalysisResponse.getQuery(), emptySet(), mlAnalysisResponse.getLogCollectionMinute(),
        ClusterLevel.getHeartBeatLevel(ClusterLevel.L2), ClusterLevel.getFinal());
    if (taskId.isPresent()) {
      learningEngineService.markCompleted(taskId.get());
    }
    return true;
  }

  @Override
  public boolean save24X7LogAnalysisRecords(String appId, String cvConfigId, int analysisMinute,
      AnalysisComparisonStrategy comparisonStrategy, LogMLAnalysisRecord mlAnalysisResponse, Optional<String> taskId) {
    final LogsCVConfiguration logsCVConfiguration = wingsPersistence.get(LogsCVConfiguration.class, cvConfigId);
    if (isNotEmpty(logsCVConfiguration.getContextId())) {
      AnalysisContext analysisContext = wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
      mlAnalysisResponse.setStateExecutionId(analysisContext.getStateExecutionId());
      mlAnalysisResponse.setStateType(analysisContext.getStateType());
    }
    mlAnalysisResponse.compressLogAnalysisRecord();
    mlAnalysisResponse.setCvConfigId(cvConfigId);
    mlAnalysisResponse.setAppId(appId);
    mlAnalysisResponse.setLogCollectionMinute(analysisMinute);
    wingsPersistence.save(mlAnalysisResponse);

    wingsPersistence.update(wingsPersistence.createQuery(LogDataRecord.class)
                                .filter("cvConfigId", cvConfigId)
                                .filter("appId", appId)
                                .filter("clusterLevel", H2)
                                .field("logCollectionMinute")
                                .lessThanOrEq(analysisMinute),
        wingsPersistence.createUpdateOperations(LogDataRecord.class).set("clusterLevel", ClusterLevel.getFinal()));
    if (taskId.isPresent()) {
      learningEngineService.markCompleted(taskId.get());
    }
    continuousVerificationService.triggerAlertIfNecessary(cvConfigId, mlAnalysisResponse.getScore(), analysisMinute);
    return true;
  }

  @Override
  public boolean saveExperimentalLogAnalysisRecords(
      ExperimentalLogMLAnalysisRecord mlAnalysisResponse, StateType stateType, Optional<String> taskId) {
    mlAnalysisResponse.setStateType(stateType);

    // replace dots in test cluster
    if (mlAnalysisResponse.getControl_clusters() != null) {
      mlAnalysisResponse.setControl_clusters(getClustersWithDotsReplaced(mlAnalysisResponse.getControl_clusters()));
    }

    // replace dots in test cluster
    if (mlAnalysisResponse.getTest_clusters() != null) {
      mlAnalysisResponse.setTest_clusters(getClustersWithDotsReplaced(mlAnalysisResponse.getTest_clusters()));
    }

    // replace dots in test cluster
    if (mlAnalysisResponse.getUnknown_clusters() != null) {
      mlAnalysisResponse.setUnknown_clusters(getClustersWithDotsReplaced(mlAnalysisResponse.getUnknown_clusters()));
    }

    // replace dots in ignored cluster
    if (mlAnalysisResponse.getIgnore_clusters() != null) {
      mlAnalysisResponse.setIgnore_clusters(getClustersWithDotsReplaced(mlAnalysisResponse.getIgnore_clusters()));
    }

    if (mlAnalysisResponse.getLogCollectionMinute() == -1 || !isEmpty(mlAnalysisResponse.getControl_events())
        || !isEmpty(mlAnalysisResponse.getTest_events())) {
      wingsPersistence.delete(wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class)
                                  .filter("appId", mlAnalysisResponse.getAppId())
                                  .filter("stateExecutionId", mlAnalysisResponse.getStateExecutionId())
                                  .filter("logCollectionMinute", mlAnalysisResponse.getLogCollectionMinute()));

      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(mlAnalysisResponse));
    }
    logger.info("inserted ml LogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getAppId()
        + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
    bumpClusterLevel(stateType, mlAnalysisResponse.getStateExecutionId(), mlAnalysisResponse.getAppId(),
        mlAnalysisResponse.getQuery(), emptySet(), mlAnalysisResponse.getLogCollectionMinute(),
        ClusterLevel.getHeartBeatLevel(ClusterLevel.L2), ClusterLevel.getFinal());
    if (taskId.isPresent()) {
      learningEngineService.markExpTaskCompleted(taskId.get());
    }
    return true;
  }

  private Map<String, Map<String, SplunkAnalysisCluster>> getClustersWithDotsReplaced(
      Map<String, Map<String, SplunkAnalysisCluster>> inputCluster) {
    if (inputCluster == null) {
      return null;
    }
    Map<String, Map<String, SplunkAnalysisCluster>> rv = new HashMap<>();
    for (Entry<String, Map<String, SplunkAnalysisCluster>> clusterEntry : inputCluster.entrySet()) {
      rv.put(clusterEntry.getKey(), new HashMap<>());
      for (Entry<String, SplunkAnalysisCluster> hostEntry : clusterEntry.getValue().entrySet()) {
        rv.get(clusterEntry.getKey()).put(Misc.replaceDotWithUnicode(hostEntry.getKey()), hostEntry.getValue());
      }
    }
    return rv;
  }

  @Override
  public LogMLAnalysisRecord getLogAnalysisRecords(
      String appId, String stateExecutionId, String query, StateType stateType, int logCollectionMinute) {
    LogMLAnalysisRecord logMLAnalysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                  .filter("stateExecutionId", stateExecutionId)
                                                  .filter("appId", appId)
                                                  .filter("query", query)
                                                  .filter("stateType", stateType)
                                                  .field("logCollectionMinute")
                                                  .lessThanOrEq(logCollectionMinute)
                                                  .order("-logCollectionMinute")
                                                  .get();
    if (logMLAnalysisRecord != null) {
      logMLAnalysisRecord.decompressLogAnalysisRecord();
    }
    return logMLAnalysisRecord;
  }

  @Override
  public LogMLAnalysisRecord getLogAnalysisRecords(String appId, String cvConfigId, int analysisMinute) {
    LogMLAnalysisRecord logMLAnalysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                  .filter("cvConfigId", cvConfigId)
                                                  .filter("appId", appId)
                                                  .field("logCollectionMinute")
                                                  .lessThanOrEq(analysisMinute)
                                                  .filter("deprecated", false)
                                                  .order("-logCollectionMinute")
                                                  .get();
    if (logMLAnalysisRecord != null) {
      logMLAnalysisRecord.decompressLogAnalysisRecord();
    }
    return logMLAnalysisRecord;
  }

  @Override
  public List<LogMLExpAnalysisInfo> getExpAnalysisInfoList() {
    final Query<ExperimentalLogMLAnalysisRecord> analysisRecords =
        wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class, excludeAuthority)
            .project("stateExecutionId", true)
            .project("appId", true)
            .project("stateType", true)
            .project("experiment_name", true)
            .project("createdAt", true)
            .project("envId", true)
            .project("workflowExecutionId", true);

    List<ExperimentalLogMLAnalysisRecord> experimentalLogMLAnalysisRecords = analysisRecords.asList();

    List<LogMLExpAnalysisInfo> result = new ArrayList<>();
    experimentalLogMLAnalysisRecords.forEach(record -> {
      result.add(LogMLExpAnalysisInfo.builder()
                     .stateExecutionId(record.getStateExecutionId())
                     .appId(record.getAppId())
                     .stateType(record.getStateType())
                     .createdAt(record.getCreatedAt())
                     .expName(record.getExperiment_name())
                     .envId(record.getEnvId())
                     .workflowExecutionId(record.getWorkflowExecutionId())
                     .build());
    });

    return result;
  }

  @Override
  public boolean reQueueExperimentalTask(String appId, String stateExecutionId) {
    final Query<LearningEngineExperimentalAnalysisTask> tasks =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class, excludeAuthority)
            .filter("state_execution_id", stateExecutionId)
            .filter("appId", appId);
    return wingsPersistence
               .update(tasks,
                   wingsPersistence.createUpdateOperations(LearningEngineExperimentalAnalysisTask.class)
                       .set("executionStatus", ExecutionStatus.QUEUED)
                       .set("retry", 0))
               .getUpdatedCount()
        > 0;
  }

  private Map<AnalysisServiceImpl.CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> getMLUserFeedbacks(
      String stateExecutionId, String appId) {
    Map<AnalysisServiceImpl.CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> userFeedbackMap = new HashMap<>();
    userFeedbackMap.put(AnalysisServiceImpl.CLUSTER_TYPE.CONTROL, new HashMap<>());
    userFeedbackMap.put(AnalysisServiceImpl.CLUSTER_TYPE.TEST, new HashMap<>());
    userFeedbackMap.put(AnalysisServiceImpl.CLUSTER_TYPE.UNKNOWN, new HashMap<>());

    PageRequest<LogMLFeedbackRecord> feedbackPageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter("stateExecutionId", Operator.EQ, stateExecutionId)
            .addFilter("appId", Operator.EQ, appId)
            .build();
    List<LogMLFeedbackRecord> logMLFeedbackRecords =
        dataStoreService.list(LogMLFeedbackRecord.class, feedbackPageRequest);

    if (logMLFeedbackRecords == null) {
      return userFeedbackMap;
    }

    for (LogMLFeedbackRecord logMLFeedbackRecord : logMLFeedbackRecords) {
      userFeedbackMap.get(logMLFeedbackRecord.getClusterType())
          .put(logMLFeedbackRecord.getClusterLabel(), logMLFeedbackRecord);
    }

    return userFeedbackMap;
  }

  private void assignUserFeedback(LogMLAnalysisSummary analysisSummary,
      Map<AnalysisServiceImpl.CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> mlUserFeedbacks) {
    for (LogMLClusterSummary summary : analysisSummary.getControlClusters()) {
      if (mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.CONTROL).containsKey(summary.getClusterLabel())) {
        summary.setLogMLFeedbackType(mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.CONTROL)
                                         .get(summary.getClusterLabel())
                                         .getLogMLFeedbackType());
        summary.setLogMLFeedbackId(
            mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.CONTROL).get(summary.getClusterLabel()).getUuid());
      }
    }

    for (LogMLClusterSummary summary : analysisSummary.getTestClusters()) {
      if (mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.TEST).containsKey(summary.getClusterLabel())) {
        summary.setLogMLFeedbackType(mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.TEST)
                                         .get(summary.getClusterLabel())
                                         .getLogMLFeedbackType());
        summary.setLogMLFeedbackId(
            mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.TEST).get(summary.getClusterLabel()).getUuid());
      }
    }

    for (LogMLClusterSummary summary : analysisSummary.getUnknownClusters()) {
      if (mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.UNKNOWN).containsKey(summary.getClusterLabel())) {
        summary.setLogMLFeedbackType(mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.UNKNOWN)
                                         .get(summary.getClusterLabel())
                                         .getLogMLFeedbackType());
        summary.setLogMLFeedbackId(
            mlUserFeedbacks.get(AnalysisServiceImpl.CLUSTER_TYPE.UNKNOWN).get(summary.getClusterLabel()).getUuid());
      }
    }
  }

  @Override
  public boolean isAnalysisPresent(String stateExecutionId, String appId) {
    LogMLAnalysisRecord analysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                             .filter("stateExecutionId", stateExecutionId)
                                             .filter("appId", appId)
                                             .order("-logCollectionMinute")
                                             .get();

    return analysisRecord != null;
  }

  @Override
  public void createAndSaveSummary(
      StateType stateType, String appId, String stateExecutionId, String query, String message) {
    final LogMLAnalysisRecord analysisRecord = LogMLAnalysisRecord.builder()
                                                   .logCollectionMinute(-1)
                                                   .stateType(stateType)
                                                   .appId(appId)
                                                   .stateExecutionId(stateExecutionId)
                                                   .query(query)
                                                   .analysisSummaryMessage(message)
                                                   .control_events(Collections.emptyMap())
                                                   .test_events(Collections.emptyMap())
                                                   .build();
    saveLogAnalysisRecords(analysisRecord, stateType, Optional.empty());
  }

  @Override
  public boolean isStateValid(String appId, String stateExecutionId) {
    return managerClientHelper.callManagerWithRetry(managerClient.isStateValid(appId, stateExecutionId)).getResource();
  }

  @Override
  public long getCollectionMinuteForLevel(String query, String appId, String stateExecutionId, StateType type,
      ClusterLevel clusterLevel, Set<String> nodes) {
    ClusterLevel heartBeat = ClusterLevel.getHeartBeatLevel(clusterLevel);

    while (true) {
      /**
       * Get the heartbeat records for L1.
       */
      try (HIterator<LogDataRecord> logHeartBeatRecordsIterator =
               new HIterator<>(wingsPersistence.createQuery(LogDataRecord.class)
                                   .filter("appId", appId)
                                   .filter("stateExecutionId", stateExecutionId)
                                   .filter("stateType", type)
                                   .filter("clusterLevel", heartBeat)
                                   .filter("query", query)
                                   .field("host")
                                   .in(nodes)
                                   .order("logCollectionMinute")
                                   .fetch())) {
        if (!logHeartBeatRecordsIterator.hasNext()) {
          return -1;
        }

        long logCollectionMinute = -1;
        Set<String> hosts;

        {
          LogDataRecord logDataRecord = logHeartBeatRecordsIterator.next();
          logCollectionMinute = logDataRecord.getLogCollectionMinute();

          hosts = Sets.newHashSet(logDataRecord.getHost());
          while (logHeartBeatRecordsIterator.hasNext()) {
            LogDataRecord dataRecord = logHeartBeatRecordsIterator.next();
            if (dataRecord.getLogCollectionMinute() == logCollectionMinute) {
              hosts.add(dataRecord.getHost());
            }
          }
        }

        if (deleteIfStale(
                query, appId, stateExecutionId, type, hosts, logCollectionMinute, ClusterLevel.L1, heartBeat)) {
          continue;
        }

        Set<String> lookupNodes = new HashSet<>(nodes);

        for (String node : hosts) {
          lookupNodes.remove(node);
        }

        if (!lookupNodes.isEmpty()) {
          logger.info(
              "Still waiting for data for " + Arrays.toString(lookupNodes.toArray()) + " for " + stateExecutionId);
        }

        return lookupNodes.isEmpty() ? logCollectionMinute : -1;
      }
    }
  }

  @Override
  public boolean hasDataRecords(String query, String appId, String stateExecutionId, StateType type, Set<String> nodes,
      ClusterLevel level, long logCollectionMinute) {
    /**
     * Get the data records for the found heartbeat.
     */
    return wingsPersistence.createQuery(LogDataRecord.class)
               .filter("appId", appId)
               .filter("stateExecutionId", stateExecutionId)
               .filter("stateType", type)
               .filter("clusterLevel", level)
               .filter("logCollectionMinute", logCollectionMinute)
               .filter("query", query)
               .field("host")
               .in(nodes)
               .count(new CountOptions().limit(1))
        > 0;
  }

  @Override
  public Optional<LogDataRecord> getHearbeatRecordForL0(
      String appId, String stateExecutionId, StateType type, String host) {
    /**
     * Find heartbeat for L0 records. L0 heartbeat is H0.
     */
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class)
                                     .filter("appId", appId)
                                     .filter("stateExecutionId", stateExecutionId)
                                     .filter("stateType", type)
                                     .filter("clusterLevel", ClusterLevel.getHeartBeatLevel(ClusterLevel.L0))
                                     .order("logCollectionMinute");

    if (isNotEmpty(host)) {
      query = query.filter("host", host);
    }

    final LogDataRecord logDataRecords = query.get();

    // Nothing more to process. break.
    return logDataRecords == null ? Optional.empty() : Optional.of(logDataRecords);
  }

  private long getLastProcessedMinute(String query, String appId, String stateExecutionId, StateType type) {
    LogDataRecord logDataRecords = wingsPersistence.createQuery(LogDataRecord.class)
                                       .filter("appId", appId)
                                       .filter("stateExecutionId", stateExecutionId)
                                       .filter("stateType", type)
                                       .filter("clusterLevel", ClusterLevel.getFinal())
                                       .filter("query", query)
                                       .order("-logCollectionMinute")
                                       .get();
    return logDataRecords == null ? -1 : logDataRecords.getLogCollectionMinute();
  }

  @Override
  public boolean isProcessingComplete(String query, String appId, String stateExecutionId, StateType type,
      int timeDurationMins, long collectionMinute, String accountId) {
    if (type.equals(StateType.SUMO)) {
      return getLastProcessedMinute(query, appId, stateExecutionId, type) - collectionMinute >= timeDurationMins - 1;
    } else {
      return getLastProcessedMinute(query, appId, stateExecutionId, type) >= timeDurationMins - 1;
    }
  }

  @Override
  public Map<String, InstanceElement> getLastExecutionNodes(String appId, String workflowId) {
    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .filter("appId", appId)
                                              .filter("workflowId", workflowId)
                                              .filter("status", SUCCESS)
                                              .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                                              .get();

    if (workflowExecution == null) {
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR, USER)
          .addParam("reason", "No successful execution exists for the workflow.");
    }

    Map<String, InstanceElement> hosts = new HashMap<>();
    for (ElementExecutionSummary executionSummary : workflowExecution.getServiceExecutionSummaries()) {
      if (isEmpty(executionSummary.getInstanceStatusSummaries())) {
        continue;
      }
      for (InstanceStatusSummary instanceStatusSummary : executionSummary.getInstanceStatusSummaries()) {
        hosts.put(instanceStatusSummary.getInstanceElement().getHostName(), instanceStatusSummary.getInstanceElement());
      }
    }
    if (isEmpty(hosts)) {
      logger.info("No nodes found for successful execution for workflow {} with executionId {}", workflowId,
          workflowExecution.getUuid());
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR, USER)
          .addParam("reason", "No node information was captured in the last successful workflow execution");
    }
    return hosts;
  }

  private boolean deleteIfStale(String query, String appId, String stateExecutionId, StateType type, Set<String> hosts,
      long logCollectionMinute, ClusterLevel clusterLevel, ClusterLevel heartBeat) {
    long lastProcessedMinute = getLastProcessedMinute(query, appId, stateExecutionId, type);
    if (logCollectionMinute <= lastProcessedMinute) {
      logger.info("deleting stale data for stateExecutionID = " + stateExecutionId + " logCollectionMinute "
          + logCollectionMinute);
      deleteClusterLevel(type, stateExecutionId, appId, query, hosts, logCollectionMinute, clusterLevel, heartBeat);
      return true;
    }
    return false;
  }

  @Override
  public long getMaxCVCollectionMinute(String appId, String cvConfigId) {
    LogDataRecord logDataRecord = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                      .filter("cvConfigId", cvConfigId)
                                      .order("-logCollectionMinute")
                                      .get();

    return logDataRecord == null ? -1 : logDataRecord.getLogCollectionMinute();
  }

  @Override
  public long getLogRecordMinute(String appId, String cvConfigId, ClusterLevel clusterLevel, OrderType orderType) {
    LogDataRecord logDataRecord =
        wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
            .filter("cvConfigId", cvConfigId)
            .filter("clusterLevel", clusterLevel)
            .order(orderType == OrderType.DESC ? "-logCollectionMinute" : "logCollectionMinute")
            .get();

    return logDataRecord == null ? -1 : logDataRecord.getLogCollectionMinute();
  }

  @Override
  public Set<String> getHostsForMinute(
      String appId, String cvConfigId, long logRecordMinute, ClusterLevel... clusterLevels) {
    Set<String> hosts = new HashSet<>();
    Set<ClusterLevel> finalClusterLevels = new HashSet<>();
    for (int i = 0; i < clusterLevels.length; i++) {
      finalClusterLevels.add(clusterLevels[i]);
      finalClusterLevels.add(ClusterLevel.getHeartBeatLevel(clusterLevels[i]));
    }

    try (HIterator<LogDataRecord> logRecordIterator =
             new HIterator<>(wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                 .filter("cvConfigId", cvConfigId)
                                 .filter("logCollectionMinute", logRecordMinute)
                                 .project("logMessage", false)
                                 .fetch())) {
      while (logRecordIterator.hasNext()) {
        final LogDataRecord logDataRecord = logRecordIterator.next();
        if (finalClusterLevels.contains(logDataRecord.getClusterLevel())) {
          hosts.add(logDataRecord.getHost());
        }
      }
    }
    return hosts;
  }

  @Override
  public long getLastCVAnalysisMinute(String appId, String cvConfigId) {
    final LogMLAnalysisRecord mlAnalysisRecord =
        wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
            .filter("cvConfigId", cvConfigId)
            .order(Sort.descending("logCollectionMinute"))
            .get();
    return mlAnalysisRecord == null ? -1 : mlAnalysisRecord.getLogCollectionMinute();
  }

  public long getLastLogDataCollectedMinute(String query, String appId, String stateExecutionId, StateType type) {
    LogDataRecord logDataRecords = wingsPersistence.createQuery(LogDataRecord.class)
                                       .filter("appId", appId)
                                       .filter("stateExecutionId", stateExecutionId)
                                       .filter("stateType", type)
                                       .filter("query", query)
                                       .order("-logCollectionMinute")
                                       .get();
    return logDataRecords == null ? -1 : logDataRecords.getLogCollectionMinute();
  }
}
