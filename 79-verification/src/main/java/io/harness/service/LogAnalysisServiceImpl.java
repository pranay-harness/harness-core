package io.harness.service;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.LT_EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.common.VerificationConstants.PER_MINUTE_CV_STATES;
import static software.wings.service.intfc.analysis.ClusterLevel.H0;
import static software.wings.service.intfc.analysis.ClusterLevel.H1;
import static software.wings.service.intfc.analysis.ClusterLevel.H2;
import static software.wings.service.intfc.analysis.ClusterLevel.HF;
import static software.wings.service.intfc.analysis.ClusterLevel.L0;
import static software.wings.service.intfc.analysis.ClusterLevel.L2;

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
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.api.InstanceElement;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData.ContinuousVerificationExecutionMetaDataKeys;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord.ExperimentalLogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.FeedbackAction;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;
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
import software.wings.service.intfc.verification.CVConfigurationService;
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
@Slf4j
public class LogAnalysisServiceImpl implements LogAnalysisService {
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
  @Inject private CVConfigurationService cvConfigurationService;

  @Override
  public void bumpClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, long logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel) {
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                     .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
                                     .filter(LogDataRecordKeys.clusterLevel, fromLevel)
                                     .filter(LogDataRecordKeys.logCollectionMinute, logCollectionMinute);

    if (isNotEmpty(host)) {
      query = query.field(LogDataRecordKeys.host).in(host);
    }
    try {
      UpdateResults results = wingsPersistence.update(query,
          wingsPersistence.createUpdateOperations(LogDataRecord.class).set(LogDataRecordKeys.clusterLevel, toLevel));
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
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                     .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
                                     .field(LogDataRecordKeys.clusterLevel)
                                     .in(asList(clusterLevels))
                                     .filter(LogDataRecordKeys.logCollectionMinute, logCollectionMinute);
    if (isNotEmpty(host)) {
      query = query.field(LogDataRecordKeys.host).in(host);
    }
    wingsPersistence.delete(query);
  }

  private void deleteClusterLevel(
      String cvConfigId, String host, int logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel) {
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                     .filter(LogDataRecordKeys.cvConfigId, cvConfigId)
                                     .filter(LogDataRecordKeys.clusterLevel, fromLevel);

    if (L2.equals(toLevel)) {
      query = query.field(LogDataRecordKeys.logCollectionMinute).lessThanOrEq(logCollectionMinute);
    } else {
      query = query.filter(LogDataRecordKeys.logCollectionMinute, logCollectionMinute);
    }

    if (isNotEmpty(host)) {
      query = query.filter(LogDataRecordKeys.host, host);
    }
    boolean deleted = wingsPersistence.delete(query);
    logger.info("Deleted clustered data for cvConfigId: {}, minute {}, fromLevel {}, toLevel {}, host {}, deleted {}",
        cvConfigId, logCollectionMinute, fromLevel, toLevel, host, deleted);
    try {
      query = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                  .filter(LogDataRecordKeys.cvConfigId, cvConfigId)
                  .filter(LogDataRecordKeys.clusterLevel, ClusterLevel.getHeartBeatLevel(fromLevel));

      if (L2.equals(toLevel)) {
        query = query.field(LogDataRecordKeys.logCollectionMinute).lessThanOrEq(logCollectionMinute);
      } else {
        query = query.filter(LogDataRecordKeys.logCollectionMinute, logCollectionMinute);
      }

      if (isNotEmpty(host)) {
        query = query.filter(LogDataRecordKeys.host, host);
      }
      UpdateResults updatedResults = wingsPersistence.update(query,
          wingsPersistence.createUpdateOperations(LogDataRecord.class)
              .set(LogDataRecordKeys.clusterLevel, ClusterLevel.getHeartBeatLevel(toLevel)));
      if (updatedResults.getUpdatedCount() == 0 && DUMMY_HOST_NAME.equals(host)) {
        logger.error("did not update heartbeat from {} to {}  for min {} host {}", fromLevel, toLevel,
            logCollectionMinute, host);
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
      if (isEmpty(cvConfigId) && !learningEngineService.isStateValid(appId, stateExecutionId)) {
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

      if (!checkIfL0AlreadyPresent(appId, cvConfigId, logDataRecords, stateExecutionId)) {
        return true;
      }
      wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);

      if (dataStoreService instanceof GoogleDataStoreServiceImpl && clusterLevel == L2) {
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
              ClusterLevel.getHeartBeatLevel(ClusterLevel.L1), ClusterLevel.getHeartBeatLevel(L2));
          deleteClusterLevel(
              stateType, stateExecutionId, appId, query, emptySet(), logCollectionMinute, ClusterLevel.L1);
          learningEngineService.markCompleted(
              workflowExecutionId, stateExecutionId, logCollectionMinute, MLAnalysisType.LOG_CLUSTER, L2);
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

  private boolean checkIfL0AlreadyPresent(
      String appId, String cvConfigId, List<LogDataRecord> logDataRecords, String stateExecutionId) {
    if (isEmpty(logDataRecords)) {
      return true;
    }
    final String fieldNameForQuery =
        isEmpty(cvConfigId) ? LogDataRecordKeys.stateExecutionId : LogDataRecordKeys.cvConfigId;
    final String fieldValueForQuery = isEmpty(cvConfigId) ? stateExecutionId : cvConfigId;

    Set<Long> clusteredMinutes = new HashSet<>();
    logDataRecords.stream()
        .filter(logDataRecord -> logDataRecord.getClusterLevel().equals(H0))
        .forEach(logDataRecord -> {
          if (isNotEmpty(getHostsForMinute(appId, fieldNameForQuery, fieldValueForQuery,
                  logDataRecord.getLogCollectionMinute(), H0, H1, H2, HF))) {
            clusteredMinutes.add(logDataRecord.getLogCollectionMinute());
          }
        });
    if (clusteredMinutes.isEmpty()) {
      return true;
    }

    logger.error("for {} got logs for minutes {} which are already clustered", fieldValueForQuery, clusteredMinutes);
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
      logger.info("Saved {} clustered data for cvConfig: {}, minute {}, toLevel {}, host {}", logDataRecords.size(),
          cvConfigId, logCollectionMinute, clusterLevel, host);

      if (dataStoreService instanceof GoogleDataStoreServiceImpl && clusterLevel.equals(L2)) {
        dataStoreService.save(LogDataRecord.class, logDataRecords, true);
        logger.info("Saved L2 clustered data to GoogleDatStore for cvConfig: {}, minute {}, toLevel {}", cvConfigId,
            logCollectionMinute, clusterLevel);
      }
    }

    switch (clusterLevel) {
      case L0:
        break;
      case L1:
        deleteClusterLevel(cvConfigId, host, logCollectionMinute, ClusterLevel.L0, ClusterLevel.L1);
        if (isEmpty(getHostsForMinute(appId, LogDataRecordKeys.cvConfigId, cvConfigId, logCollectionMinute, L0, H0))) {
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
        deleteClusterLevel(cvConfigId, null, logCollectionMinute, ClusterLevel.L1, L2);
        learningEngineService.markCompleted(null, "LOGS_CLUSTER_L2_" + cvConfigId + "_" + logCollectionMinute,
            logCollectionMinute, MLAnalysisType.LOG_CLUSTER, L2);
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
      recordQuery = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                        .filter(LogDataRecordKeys.stateExecutionId, logRequest.getStateExecutionId())
                        .filter(LogDataRecordKeys.clusterLevel, clusterLevel)
                        .filter(LogDataRecordKeys.logCollectionMinute, logRequest.getLogCollectionMinute())
                        .field(LogDataRecordKeys.host)
                        .hasAnyOf(logRequest.getNodes());

    } else {
      long timeDelta = 0;

      if (PER_MINUTE_CV_STATES.contains(stateType)) {
        LogDataRecord logDataRecord = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                          .project(LogDataRecordKeys.logCollectionMinute, true)
                                          .filter(LogDataRecordKeys.stateExecutionId, logRequest.getStateExecutionId())
                                          .filter(LogDataRecordKeys.clusterLevel, clusterLevel)
                                          .order(Sort.ascending(LogDataRecordKeys.logCollectionMinute))
                                          .get();

        if (logDataRecord != null) {
          timeDelta = logRequest.getLogCollectionMinute() - logDataRecord.getLogCollectionMinute();
        }

        logDataRecord = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                            .project(LogDataRecordKeys.logCollectionMinute, true)
                            .filter(LogDataRecordKeys.workflowExecutionId, workflowExecutionId)
                            .filter(LogDataRecordKeys.serviceId, logRequest.getServiceId())
                            .filter(LogDataRecordKeys.stateType, stateType)
                            .filter(LogDataRecordKeys.query, logRequest.getQuery())
                            .filter(LogDataRecordKeys.clusterLevel, clusterLevel)
                            .order(Sort.ascending(LogDataRecordKeys.logCollectionMinute))
                            .get();

        if (logDataRecord != null) {
          logRequest.setLogCollectionMinute(timeDelta + logDataRecord.getLogCollectionMinute());
        }
      }

      recordQuery = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                        .filter(LogDataRecordKeys.workflowExecutionId, workflowExecutionId)
                        .filter(LogDataRecordKeys.serviceId, logRequest.getServiceId())
                        .filter(LogDataRecordKeys.stateType, stateType)
                        .filter(LogDataRecordKeys.query, logRequest.getQuery())
                        .filter(LogDataRecordKeys.clusterLevel, clusterLevel)
                        .filter(LogDataRecordKeys.logCollectionMinute, logRequest.getLogCollectionMinute());
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
                                                 .addFilter(LogDataRecordKeys.cvConfigId, Operator.EQ, cvConfigId)
                                                 .addFilter(LogDataRecordKeys.clusterLevel, Operator.EQ, clusterLevel)
                                                 .build();

    if (logCollectionMinute > 0) {
      pageRequest.addFilter(LogDataRecordKeys.logCollectionMinute, Operator.EQ, logCollectionMinute);
    } else {
      pageRequest.addFilter(LogDataRecordKeys.logCollectionMinute, Operator.GE, startMinute);
      pageRequest.addFilter(LogDataRecordKeys.logCollectionMinute, LT_EQ, endMinute);
    }

    if (isNotEmpty(logRequest.getNodes())) {
      pageRequest.addFilter(LogDataRecordKeys.host, Operator.IN, logRequest.getNodes().toArray());
    }

    PageResponse<LogDataRecord> response = wingsPersistence.query(LogDataRecord.class, pageRequest, excludeAuthority);
    while (!response.isEmpty()) {
      logDataRecords.addAll(response.getResponse());

      previousOffset += response.size();
      pageRequest.setOffset(String.valueOf(previousOffset));
      response = wingsPersistence.query(LogDataRecord.class, pageRequest, excludeAuthority);
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
    Query<LogDataRecord> splunkLogDataRecordQuery =
        wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
            .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
            .filter(LogDataRecordKeys.logCollectionMinute, logCollectionMinute);
    return !splunkLogDataRecordQuery.asList().isEmpty();
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithLogs(
      StateType stateType, String appId, String serviceId, String workflowId, String query) {
    // TODO should we limit the number of executions to search in ??
    List<String> successfulExecutions = new ArrayList<>();
    List<ContinuousVerificationExecutionMetaData> cvList =
        wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class, excludeAuthority)
            .filter(ContinuousVerificationExecutionMetaDataKeys.workflowId, workflowId)
            .filter(ContinuousVerificationExecutionMetaDataKeys.stateType, stateType)
            .filter(ContinuousVerificationExecutionMetaDataKeys.executionStatus, ExecutionStatus.SUCCESS)
            .order(Sort.descending(ContinuousVerificationExecutionMetaDataKeys.workflowStartTs))
            .asList();
    cvList.forEach(cvMetadata -> successfulExecutions.add(cvMetadata.getWorkflowExecutionId()));
    for (String successfulExecution : successfulExecutions) {
      if (wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
              .filter(LogDataRecordKeys.workflowExecutionId, successfulExecution)
              .filter(LogDataRecordKeys.clusterLevel, L2)
              .filter(LogDataRecordKeys.query, query)
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
        ClusterLevel.getHeartBeatLevel(L2), ClusterLevel.getFinal());
    if (taskId.isPresent()) {
      learningEngineService.markCompleted(taskId.get());
    }
    return true;
  }

  @Override
  public boolean save24X7LogAnalysisRecords(String appId, String cvConfigId, int analysisMinute,
      AnalysisComparisonStrategy comparisonStrategy, LogMLAnalysisRecord mlAnalysisResponse, Optional<String> taskId,
      Optional<Boolean> isFeedbackAnalysis) {
    final LogsCVConfiguration logsCVConfiguration = wingsPersistence.get(LogsCVConfiguration.class, cvConfigId);
    if (isNotEmpty(logsCVConfiguration.getContextId())) {
      AnalysisContext analysisContext = wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
      mlAnalysisResponse.setStateExecutionId(analysisContext.getStateExecutionId());
      mlAnalysisResponse.setStateType(analysisContext.getStateType());
    }
    Map<String, Map<String, SplunkAnalysisCluster>> unknownClusters = mlAnalysisResponse.getUnknown_clusters();
    LogMLAnalysisRecord uncompressedAnalysis = mlAnalysisResponse;
    mlAnalysisResponse.compressLogAnalysisRecord();
    mlAnalysisResponse.setCvConfigId(cvConfigId);
    mlAnalysisResponse.setAppId(appId);
    mlAnalysisResponse.setLogCollectionMinute(analysisMinute);
    if (!isFeedbackAnalysis.isPresent() || !isFeedbackAnalysis.get()) {
      mlAnalysisResponse.setAnalysisStatus(LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);
    } else {
      mlAnalysisResponse.setAnalysisStatus(LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);
    }
    wingsPersistence.save(mlAnalysisResponse);
    mlAnalysisResponse.setUnknown_clusters(unknownClusters);

    wingsPersistence.update(wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                .filter(LogDataRecordKeys.cvConfigId, cvConfigId)
                                .filter(LogDataRecordKeys.clusterLevel, H2)
                                .field(LogDataRecordKeys.logCollectionMinute)
                                .lessThanOrEq(analysisMinute),
        wingsPersistence.createUpdateOperations(LogDataRecord.class)
            .set(LogDataRecordKeys.clusterLevel, ClusterLevel.getFinal()));
    wingsPersistence.delete(wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                .filter(LogDataRecordKeys.cvConfigId, cvConfigId)
                                .filter(LogDataRecordKeys.clusterLevel, L2)
                                .field(LogDataRecordKeys.logCollectionMinute)
                                .lessThanOrEq(analysisMinute));
    if (taskId.isPresent()) {
      learningEngineService.markCompleted(taskId.get());
    }
    continuousVerificationService.triggerLogAnalysisAlertIfNecessary(cvConfigId, mlAnalysisResponse, analysisMinute);
    return true;
  }

  @Override
  public Map<FeedbackAction, List<CVFeedbackRecord>> getUserFeedback(
      String cvConfigId, String stateExecutionId, String appId) {
    List<CVFeedbackRecord> feedbackRecords =
        managerClientHelper.callManagerWithRetry(managerClient.getFeedbackList(cvConfigId, stateExecutionId))
            .getResource();

    Map<FeedbackAction, List<CVFeedbackRecord>> actionRecordMap = new HashMap<>();
    for (FeedbackAction action : FeedbackAction.values()) {
      actionRecordMap.put(action, new ArrayList<>());
    }
    feedbackRecords.forEach(feedbackRecord -> {
      FeedbackAction actionTaken = feedbackRecord.getActionTaken();
      actionRecordMap.get(actionTaken).add(feedbackRecord);
    });
    return actionRecordMap;
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
      wingsPersistence.delete(
          wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class)
              .filter("appId", mlAnalysisResponse.getAppId())
              .filter(ExperimentalLogMLAnalysisRecordKeys.stateExecutionId, mlAnalysisResponse.getStateExecutionId())
              .filter(ExperimentalLogMLAnalysisRecordKeys.logCollectionMinute,
                  mlAnalysisResponse.getLogCollectionMinute()));

      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(mlAnalysisResponse));
    }
    logger.info("inserted ml LogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getAppId()
        + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
    bumpClusterLevel(stateType, mlAnalysisResponse.getStateExecutionId(), mlAnalysisResponse.getAppId(),
        mlAnalysisResponse.getQuery(), emptySet(), mlAnalysisResponse.getLogCollectionMinute(),
        ClusterLevel.getHeartBeatLevel(L2), ClusterLevel.getFinal());
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
                                                  .filter(LogMLAnalysisRecordKeys.stateExecutionId, stateExecutionId)
                                                  .filter("appId", appId)
                                                  .filter(LogMLAnalysisRecordKeys.query, query)
                                                  .filter(LogMLAnalysisRecordKeys.stateType, stateType)
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
                                                  .filter(LogMLAnalysisRecordKeys.cvConfigId, cvConfigId)
                                                  .filter("appId", appId)
                                                  .field("logCollectionMinute")
                                                  .lessThanOrEq(analysisMinute)
                                                  .filter(LogMLAnalysisRecordKeys.deprecated, false)
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
    LogMLAnalysisRecord analysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                             .filter(LogMLAnalysisRecordKeys.stateExecutionId, stateExecutionId)
                                             .order(Sort.descending(LogDataRecordKeys.logCollectionMinute))
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
  public long getCollectionMinuteForLevel(String query, String appId, String stateExecutionId, StateType type,
      ClusterLevel clusterLevel, Set<String> nodes) {
    ClusterLevel heartBeat = ClusterLevel.getHeartBeatLevel(clusterLevel);

    while (true) {
      /**
       * Get the heartbeat records for L1.
       */
      try (HIterator<LogDataRecord> logHeartBeatRecordsIterator =
               new HIterator<>(wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                   .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
                                   .filter(LogDataRecordKeys.clusterLevel, heartBeat)
                                   .field(LogDataRecordKeys.host)
                                   .in(nodes)
                                   .order(Sort.ascending(LogDataRecordKeys.logCollectionMinute))
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
    return wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
               .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
               .filter(LogDataRecordKeys.clusterLevel, level)
               .filter(LogDataRecordKeys.logCollectionMinute, logCollectionMinute)
               .field(LogDataRecordKeys.host)
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
    Query<LogDataRecord> query =
        wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
            .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
            .filter(LogDataRecordKeys.clusterLevel, ClusterLevel.getHeartBeatLevel(ClusterLevel.L0))
            .order(Sort.ascending(LogDataRecordKeys.logCollectionMinute));

    if (isNotEmpty(host)) {
      query = query.filter(LogDataRecordKeys.host, host);
    }

    final LogDataRecord logDataRecords = query.get();

    // Nothing more to process. break.
    return logDataRecords == null ? Optional.empty() : Optional.of(logDataRecords);
  }

  private long getLastProcessedMinute(String stateExecutionId) {
    LogDataRecord logDataRecords = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                       .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
                                       .filter(LogDataRecordKeys.clusterLevel, ClusterLevel.getFinal())
                                       .order(Sort.descending(LogDataRecordKeys.logCollectionMinute))
                                       .get();
    return logDataRecords == null ? -1 : logDataRecords.getLogCollectionMinute();
  }

  @Override
  public boolean isProcessingComplete(String query, String appId, String stateExecutionId, StateType type,
      int timeDurationMins, long collectionMinute, String accountId) {
    if (PER_MINUTE_CV_STATES.contains(type)) {
      return getLastProcessedMinute(stateExecutionId) - collectionMinute >= timeDurationMins - 1;
    } else {
      return getLastProcessedMinute(stateExecutionId) >= timeDurationMins - 1;
    }
  }

  @Override
  public Map<String, InstanceElement> getLastExecutionNodes(String appId, String workflowId) {
    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .filter("appId", appId)
                                              .filter(WorkflowExecutionKeys.workflowId, workflowId)
                                              .filter(WorkflowExecutionKeys.status, SUCCESS)
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
    long lastProcessedMinute = getLastProcessedMinute(stateExecutionId);
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
                                      .filter(LogDataRecordKeys.cvConfigId, cvConfigId)
                                      .order(Sort.descending(LogDataRecordKeys.logCollectionMinute))
                                      .get();

    return logDataRecord == null ? -1 : logDataRecord.getLogCollectionMinute();
  }

  @Override
  public long getLogRecordMinute(String appId, String cvConfigId, ClusterLevel clusterLevel, OrderType orderType) {
    LogDataRecord logDataRecord =
        wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
            .filter(LogDataRecordKeys.cvConfigId, cvConfigId)
            .filter(LogDataRecordKeys.clusterLevel, clusterLevel)
            .order(orderType == OrderType.DESC ? Sort.descending(LogDataRecordKeys.logCollectionMinute)
                                               : Sort.ascending(LogDataRecordKeys.logCollectionMinute))
            .get();

    return logDataRecord == null ? -1 : logDataRecord.getLogCollectionMinute();
  }

  @Override
  public Set<String> getHostsForMinute(String appId, String fieldNameForQuery, String fieldValueForQuery,
      long logRecordMinute, ClusterLevel... clusterLevels) {
    Set<String> hosts = new HashSet<>();
    Set<ClusterLevel> finalClusterLevels = Sets.newHashSet(clusterLevels);

    Query<LogDataRecord> logDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                                  .filter(fieldNameForQuery, fieldValueForQuery)
                                                  .filter(LogDataRecordKeys.logCollectionMinute, logRecordMinute)
                                                  .project(LogDataRecordKeys.logMessage, false);

    try (HIterator<LogDataRecord> logRecordIterator = new HIterator<>(logDataRecordQuery.fetch())) {
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
  public long getLastCVAnalysisMinute(String appId, String cvConfigId, LogMLAnalysisStatus status) {
    // TODO; Write migration to add analysis status to all analyses in the past one month.
    final LogMLAnalysisRecord mlAnalysisRecord =
        wingsPersistence
            .createQuery(LogMLAnalysisRecord.class, excludeAuthority)

            .filter(LogMLAnalysisRecordKeys.cvConfigId, cvConfigId)
            .filter(LogMLAnalysisRecordKeys.analysisStatus, status)
            .order(Sort.descending(LogMLAnalysisRecordKeys.logCollectionMinute))

            .get();
    return mlAnalysisRecord == null ? -1 : mlAnalysisRecord.getLogCollectionMinute();
  }

  public long getLastLogDataCollectedMinute(String query, String appId, String stateExecutionId, StateType type) {
    LogDataRecord logDataRecords = wingsPersistence.createQuery(LogDataRecord.class, excludeAuthority)
                                       .filter(LogDataRecordKeys.stateExecutionId, stateExecutionId)
                                       .order(Sort.descending(LogDataRecordKeys.logCollectionMinute))
                                       .get();
    return logDataRecords == null ? -1 : logDataRecords.getLogCollectionMinute();
  }

  public boolean updateAnalysisStatus(String cvConfigId, long analysisMinute, LogMLAnalysisStatus status) {
    final Query<LogMLAnalysisRecord> query = wingsPersistence.createQuery(LogMLAnalysisRecord.class, excludeAuthority)
                                                 .filter(LogMLAnalysisRecordKeys.cvConfigId, cvConfigId)
                                                 .filter(LogMLAnalysisRecordKeys.logCollectionMinute, analysisMinute);

    try {
      UpdateResults results = wingsPersistence.update(query,
          wingsPersistence.createUpdateOperations(LogMLAnalysisRecord.class)
              .set(LogMLAnalysisRecordKeys.analysisStatus, status));
      logger.info("for {} and minute {} bumped the analysisStatus to  {}", cvConfigId, analysisMinute, status);
      return true;
    } catch (DuplicateKeyException e) {
      logger.info("for {} and minute {} Failed to bump the analysisStatus to  {}", cvConfigId, analysisMinute, status);
      return false;
    }
  }
}
