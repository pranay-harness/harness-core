package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.utils.Switch.noop;
import static software.wings.utils.Switch.unhandled;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.PhaseElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.config.LogzConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.splunk.LogMLClusterScores;
import software.wings.service.impl.splunk.LogMLClusterScores.LogMLScore;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.service.intfc.sumo.SumoDelegateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
public class AnalysisServiceImpl implements AnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);
  private static final double HIGH_RISK_THRESHOLD = 50;
  private static final double MEDIUM_RISK_THRESHOLD = 25;

  private final Random random = new Random();

  public static final StateType[] logAnalysisStates = new StateType[] {StateType.SPLUNKV2, StateType.ELK};

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject protected SettingsService settingsService;
  @Inject protected WorkflowExecutionService workflowExecutionService;
  @Inject protected MainConfiguration configuration;
  @Inject protected DelegateServiceImpl delegateService;
  @Inject protected SecretManager secretManager;
  @Inject private LearningEngineService learningEngineService;

  @Override
  public void bumpClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, int logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel) {
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class)
                                     .field("stateType")
                                     .equal(stateType)
                                     .field("stateExecutionId")
                                     .equal(stateExecutionId)
                                     .field("applicationId")
                                     .equal(appId)
                                     .field("query")
                                     .equal(searchQuery)
                                     .field("logCollectionMinute")
                                     .equal(logCollectionMinute)
                                     .field("clusterLevel")
                                     .equal(fromLevel);

    if (isNotEmpty(host)) {
      query = query.field("host").in(host);
    }
    wingsPersistence.update(
        query, wingsPersistence.createUpdateOperations(LogDataRecord.class).set("clusterLevel", toLevel));
  }

  @Override
  public void deleteClusterLevel(StateType stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, int logCollectionMinute, ClusterLevel... clusterLevels) {
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class)
                                     .field("stateType")
                                     .equal(stateType)
                                     .field("stateExecutionId")
                                     .equal(stateExecutionId)
                                     .field("applicationId")
                                     .equal(appId)
                                     .field("query")
                                     .equal(searchQuery)
                                     .field("logCollectionMinute")
                                     .equal(logCollectionMinute)
                                     .field("clusterLevel")
                                     .in(asList(clusterLevels));
    if (isNotEmpty(host)) {
      query = query.field("host").in(host);
    }
    wingsPersistence.delete(query);
  }

  @Override
  public Boolean saveLogData(StateType stateType, String accountId, String appId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, ClusterLevel clusterLevel, String delegateTaskId,
      List<LogElement> logData) {
    try {
      if (!isStateValid(appId, stateExecutionId)) {
        logger.warn(
            "State is no longer active " + stateExecutionId + ". Sending delegate abort request " + delegateTaskId);
        delegateService.abortTask(accountId, delegateTaskId);
        return false;
      }
      logger.info("inserting " + logData.size() + " pieces of log data");

      if (logData.isEmpty()) {
        return true;
      }

      boolean hasHeartBeat = Integer.parseInt(logData.get(0).getClusterLabel()) < 0;

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
          LogDataRecord.generateDataRecords(stateType, appId, stateExecutionId, workflowId, workflowExecutionId,
              serviceId, clusterLevel, ClusterLevel.getHeartBeatLevel(clusterLevel), logData);
      wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);
      logger.info("inserted " + logDataRecords.size() + " LogDataRecord to persistence layer.");

      // bump the level for clustered data
      int logCollectionMinute = logData.get(0).getLogCollectionMinute();
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
          bumpClusterLevel(stateType, stateExecutionId, appId, query, Collections.emptySet(), logCollectionMinute,
              ClusterLevel.getHeartBeatLevel(ClusterLevel.L1), ClusterLevel.getHeartBeatLevel(ClusterLevel.L2));
          deleteClusterLevel(
              stateType, stateExecutionId, appId, query, Collections.emptySet(), logCollectionMinute, ClusterLevel.L1);
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

  @Override
  public List<LogDataRecord> getLogData(LogRequest logRequest, boolean compareCurrent, String workflowExecutionId,
      ClusterLevel clusterLevel, StateType stateType) {
    List<LogDataRecord> records;
    if (compareCurrent) {
      records = wingsPersistence.createQuery(LogDataRecord.class)
                    .field("stateType")
                    .equal(stateType)
                    .field("stateExecutionId")
                    .equal(logRequest.getStateExecutionId())
                    .field("workflowExecutionId")
                    .equal(workflowExecutionId)
                    .field("applicationId")
                    .equal(logRequest.getApplicationId())
                    .field("query")
                    .equal(logRequest.getQuery())
                    .field("serviceId")
                    .equal(logRequest.getServiceId())
                    .field("clusterLevel")
                    .equal(clusterLevel)
                    .field("logCollectionMinute")
                    .equal(logRequest.getLogCollectionMinute())
                    .field("host")
                    .hasAnyOf(logRequest.getNodes())
                    .asList();

    } else {
      records = wingsPersistence.createQuery(LogDataRecord.class)
                    .field("stateType")
                    .equal(stateType)
                    .field("workflowExecutionId")
                    .equal(workflowExecutionId)
                    .field("applicationId")
                    .equal(logRequest.getApplicationId())
                    .field("query")
                    .equal(logRequest.getQuery())
                    .field("serviceId")
                    .equal(logRequest.getServiceId())
                    .field("clusterLevel")
                    .equal(clusterLevel)
                    .field("logCollectionMinute")
                    .equal(logRequest.getLogCollectionMinute())
                    .asList();
    }

    logger.debug("returning " + records.size() + " records for request: " + logRequest);
    return records;
  }

  @Override
  public boolean saveFeedback(LogMLFeedback feedback, StateType stateType) {
    String logmd5Hash = DigestUtils.md5Hex(feedback.getText());

    if (!isEmpty(feedback.getLogMlFeedbackId())) {
      Query<LogMLFeedbackRecord> query =
          wingsPersistence.createQuery(LogMLFeedbackRecord.class).field("_id").equal(feedback.getLogMlFeedbackId());

      wingsPersistence.delete(query);
    }

    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, feedback.getAppId(), feedback.getStateExecutionId());

    if (stateExecutionInstance == null) {
      throw new WingsException("Unable to find state execution for id " + stateExecutionInstance.getUuid());
    }

    Optional<ContextElement> optionalElement = stateExecutionInstance.getContextElements()
                                                   .stream()
                                                   .filter(contextElement -> contextElement instanceof PhaseElement)
                                                   .findFirst();
    if (!optionalElement.isPresent()) {
      throw new WingsException(
          "Unable to find phase element for state execution id " + stateExecutionInstance.getUuid());
    }

    PhaseElement phaseElement = (PhaseElement) optionalElement.get();

    LogMLFeedbackRecord mlFeedbackRecord = LogMLFeedbackRecord.builder()
                                               .applicationId(feedback.getAppId())
                                               .serviceId(phaseElement.getServiceElement().getUuid())
                                               .workflowId(stateExecutionInstance.getWorkflowId())
                                               .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
                                               .stateExecutionId(feedback.getStateExecutionId())
                                               .logMessage(feedback.getText())
                                               .logMLFeedbackType(feedback.getLogMLFeedbackType())
                                               .clusterLabel(feedback.getClusterLabel())
                                               .clusterType(feedback.getClusterType())
                                               .logMD5Hash(logmd5Hash)
                                               .stateType(stateType)
                                               .comment(feedback.getComment())
                                               .build();

    wingsPersistence.save(mlFeedbackRecord);

    return true;
  }

  @Override
  public boolean isLogDataCollected(
      String applicationId, String stateExecutionId, String query, int logCollectionMinute, StateType stateType) {
    Query<LogDataRecord> splunkLogDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                                        .field("stateType")
                                                        .equal(stateType)
                                                        .field("stateExecutionId")
                                                        .equal(stateExecutionId)
                                                        .field("applicationId")
                                                        .equal(applicationId)
                                                        .field("query")
                                                        .equal(query)
                                                        .field("logCollectionMinute")
                                                        .equal(logCollectionMinute);
    return !splunkLogDataRecordQuery.asList().isEmpty();
  }

  @Override
  public boolean isBaselineCreated(AnalysisComparisonStrategy comparisonStrategy, StateType stateType,
      String applicationId, String workflowId, String workflowExecutionId, String serviceId, String query) {
    if (comparisonStrategy == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
      return true;
    }
    return !getLastSuccessfulWorkflowExecutionIdWithLogs(stateType, applicationId, serviceId, query, workflowId)
                .equals("-1");
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithLogs(
      StateType stateType, String appId, String serviceId, String query, String workflowId) {
    // TODO should we limit the number of executions to search in ??
    List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(appId, workflowId);
    for (String successfulExecution : successfulExecutions) {
      Query<LogDataRecord> lastSuccessfulRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                                           .field("stateType")
                                                           .equal(stateType)
                                                           .field("workflowId")
                                                           .equal(workflowId)
                                                           .field("workflowExecutionId")
                                                           .equal(successfulExecution)
                                                           .field("serviceId")
                                                           .equal(serviceId)
                                                           .field("query")
                                                           .equal(query)
                                                           .field("clusterLevel")
                                                           .equal(ClusterLevel.L2)
                                                           .limit(1);

      List<LogDataRecord> lastSuccessfulRecords = lastSuccessfulRecordQuery.asList();
      if (isNotEmpty(lastSuccessfulRecords)) {
        return successfulExecution;
      }
    }
    logger.warn("Could not get a successful workflow to find control nodes");
    return "-1";
  }

  private List<String> getLastSuccessfulWorkflowExecutionIds(String appId, String workflowId) {
    final PageRequest<WorkflowExecution> pageRequest = PageRequest.Builder.aPageRequest()
                                                           .addFilter("appId", Operator.EQ, appId)
                                                           .addFilter("workflowId", Operator.EQ, workflowId)
                                                           .addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS)
                                                           .addOrder("createdAt", OrderType.DESC)
                                                           .build();

    final PageResponse<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, false);
    final List<String> workflowExecutionIds = new ArrayList<>();

    if (workflowExecutions != null) {
      for (WorkflowExecution workflowExecution : workflowExecutions) {
        workflowExecutionIds.add(workflowExecution.getUuid());
      }
    }
    return workflowExecutionIds;
  }

  @Override
  public Boolean saveLogAnalysisRecords(
      LogMLAnalysisRecord mlAnalysisResponse, StateType stateType, Optional<String> taskId) {
    mlAnalysisResponse.setStateType(stateType);

    // replace dots in test cluster
    if (mlAnalysisResponse.getControl_clusters() != null) {
      Map<String, Map<String, SplunkAnalysisCluster>> controlClustersMap = new HashMap<>();
      for (Entry<String, Map<String, SplunkAnalysisCluster>> clusterEntry :
          mlAnalysisResponse.getControl_clusters().entrySet()) {
        controlClustersMap.put(clusterEntry.getKey(), new HashMap<>());
        for (Entry<String, SplunkAnalysisCluster> hostEntry : clusterEntry.getValue().entrySet()) {
          controlClustersMap.get(clusterEntry.getKey())
              .put(Misc.replaceDotWithUnicode(hostEntry.getKey()), hostEntry.getValue());
        }
      }
      mlAnalysisResponse.setControl_clusters(controlClustersMap);
    }

    // replace dots in test cluster
    if (mlAnalysisResponse.getTest_clusters() != null) {
      Map<String, Map<String, SplunkAnalysisCluster>> testClustersMap = new HashMap<>();
      for (Entry<String, Map<String, SplunkAnalysisCluster>> clusterEntry :
          mlAnalysisResponse.getTest_clusters().entrySet()) {
        testClustersMap.put(clusterEntry.getKey(), new HashMap<>());
        for (Entry<String, SplunkAnalysisCluster> hostEntry : clusterEntry.getValue().entrySet()) {
          testClustersMap.get(clusterEntry.getKey())
              .put(Misc.replaceDotWithUnicode(hostEntry.getKey()), hostEntry.getValue());
        }
      }
      mlAnalysisResponse.setTest_clusters(testClustersMap);
    }

    // replace dots in test cluster
    if (mlAnalysisResponse.getUnknown_clusters() != null) {
      Map<String, Map<String, SplunkAnalysisCluster>> unknownClustersMap = new HashMap<>();
      for (Entry<String, Map<String, SplunkAnalysisCluster>> clusterEntry :
          mlAnalysisResponse.getUnknown_clusters().entrySet()) {
        unknownClustersMap.put(clusterEntry.getKey(), new HashMap<>());
        for (Entry<String, SplunkAnalysisCluster> hostEntry : clusterEntry.getValue().entrySet()) {
          unknownClustersMap.get(clusterEntry.getKey())
              .put(Misc.replaceDotWithUnicode(hostEntry.getKey()), hostEntry.getValue());
        }
      }
      mlAnalysisResponse.setUnknown_clusters(unknownClustersMap);
    }

    if (mlAnalysisResponse.getLogCollectionMinute() == -1 || !isEmpty(mlAnalysisResponse.getControl_events())
        || !isEmpty(mlAnalysisResponse.getTest_events())) {
      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(mlAnalysisResponse));
    }
    logger.debug(
        "inserted ml LogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getApplicationId()
        + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
    bumpClusterLevel(stateType, mlAnalysisResponse.getStateExecutionId(), mlAnalysisResponse.getApplicationId(),
        mlAnalysisResponse.getQuery(), Collections.emptySet(), mlAnalysisResponse.getLogCollectionMinute(),
        ClusterLevel.getHeartBeatLevel(ClusterLevel.L2), ClusterLevel.getFinal());
    if (taskId.isPresent()) {
      learningEngineService.markCompleted(taskId.get());
    }
    return true;
  }

  @Override
  public LogMLAnalysisRecord getLogAnalysisRecords(
      String applicationId, String stateExecutionId, String query, StateType stateType, Integer logCollectionMinute) {
    Iterator<LogMLAnalysisRecord> iteratorAnalysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                               .field("stateExecutionId")
                                                               .equal(stateExecutionId)
                                                               .field("applicationId")
                                                               .equal(applicationId)
                                                               .field("query")
                                                               .equal(query)
                                                               .field("stateType")
                                                               .equal(stateType)
                                                               .field("logCollectionMinute")
                                                               .lessThanOrEq(logCollectionMinute)
                                                               .order("-logCollectionMinute")
                                                               .fetch(new FindOptions().limit(1));

    return iteratorAnalysisRecord.hasNext() ? iteratorAnalysisRecord.next() : null;
  }

  private Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> getMLUserFeedbacks(
      String stateExecutionId, String applicationId) {
    Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> userFeedbackMap = new HashMap<>();
    userFeedbackMap.put(CLUSTER_TYPE.CONTROL, new HashMap<>());
    userFeedbackMap.put(CLUSTER_TYPE.TEST, new HashMap<>());
    userFeedbackMap.put(CLUSTER_TYPE.UNKNOWN, new HashMap<>());

    List<LogMLFeedbackRecord> logMLFeedbackRecords = wingsPersistence.createQuery(LogMLFeedbackRecord.class)
                                                         .field("stateExecutionId")
                                                         .equal(stateExecutionId)
                                                         .field("applicationId")
                                                         .equal(applicationId)
                                                         .asList();

    if (logMLFeedbackRecords == null) {
      return userFeedbackMap;
    }

    for (LogMLFeedbackRecord logMLFeedbackRecord : logMLFeedbackRecords) {
      userFeedbackMap.get(logMLFeedbackRecord.getClusterType())
          .put(logMLFeedbackRecord.getClusterLabel(), logMLFeedbackRecord);
    }

    return userFeedbackMap;
  }

  private void assignUserFeedback(
      LogMLAnalysisSummary analysisSummary, Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> mlUserFeedbacks) {
    for (LogMLClusterSummary summary : analysisSummary.getControlClusters()) {
      if (mlUserFeedbacks.get(CLUSTER_TYPE.CONTROL).containsKey(summary.getClusterLabel())) {
        summary.setLogMLFeedbackType(
            mlUserFeedbacks.get(CLUSTER_TYPE.CONTROL).get(summary.getClusterLabel()).getLogMLFeedbackType());
        summary.setLogMLFeedbackId(mlUserFeedbacks.get(CLUSTER_TYPE.CONTROL).get(summary.getClusterLabel()).getUuid());
      }
    }

    for (LogMLClusterSummary summary : analysisSummary.getTestClusters()) {
      if (mlUserFeedbacks.get(CLUSTER_TYPE.TEST).containsKey(summary.getClusterLabel())) {
        summary.setLogMLFeedbackType(
            mlUserFeedbacks.get(CLUSTER_TYPE.TEST).get(summary.getClusterLabel()).getLogMLFeedbackType());
        summary.setLogMLFeedbackId(mlUserFeedbacks.get(CLUSTER_TYPE.TEST).get(summary.getClusterLabel()).getUuid());
      }
    }

    for (LogMLClusterSummary summary : analysisSummary.getUnknownClusters()) {
      if (mlUserFeedbacks.get(CLUSTER_TYPE.UNKNOWN).containsKey(summary.getClusterLabel())) {
        summary.setLogMLFeedbackType(
            mlUserFeedbacks.get(CLUSTER_TYPE.UNKNOWN).get(summary.getClusterLabel()).getLogMLFeedbackType());
        summary.setLogMLFeedbackId(mlUserFeedbacks.get(CLUSTER_TYPE.UNKNOWN).get(summary.getClusterLabel()).getUuid());
      }
    }
  }

  @Override
  public LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String applicationId, StateType stateType) {
    Iterator<LogMLAnalysisRecord> iteratorAnalysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                               .field("stateExecutionId")
                                                               .equal(stateExecutionId)
                                                               .field("applicationId")
                                                               .equal(applicationId)
                                                               .field("stateType")
                                                               .equal(stateType)
                                                               .order("-logCollectionMinute")
                                                               .fetch(new FindOptions().limit(1));

    if (!iteratorAnalysisRecord.hasNext()) {
      return null;
    }

    Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> mlUserFeedbacks =
        getMLUserFeedbacks(stateExecutionId, applicationId);

    LogMLAnalysisRecord analysisRecord = iteratorAnalysisRecord.next();
    final LogMLAnalysisSummary analysisSummary = new LogMLAnalysisSummary();
    analysisSummary.setQuery(analysisRecord.getQuery());
    analysisSummary.setScore(analysisRecord.getScore() * 100);
    analysisSummary.setControlClusters(
        computeCluster(analysisRecord.getControl_clusters(), Collections.emptyMap(), CLUSTER_TYPE.CONTROL));
    LogMLClusterScores logMLClusterScores =
        analysisRecord.getCluster_scores() != null ? analysisRecord.getCluster_scores() : new LogMLClusterScores();
    analysisSummary.setTestClusters(
        computeCluster(analysisRecord.getTest_clusters(), logMLClusterScores.getTest(), CLUSTER_TYPE.TEST));
    analysisSummary.setUnknownClusters(
        computeCluster(analysisRecord.getUnknown_clusters(), logMLClusterScores.getUnknown(), CLUSTER_TYPE.UNKNOWN));

    if (!analysisRecord.isBaseLineCreated()) {
      analysisSummary.setTestClusters(analysisSummary.getControlClusters());
      analysisSummary.setControlClusters(new ArrayList<>());
    }

    assignUserFeedback(analysisSummary, mlUserFeedbacks);

    RiskLevel riskLevel = RiskLevel.NA;
    String analysisSummaryMsg = isEmpty(analysisRecord.getAnalysisSummaryMessage())
        ? analysisSummary.getControlClusters().isEmpty() ? "No baseline data for the given query was found."
                                                         : analysisSummary.getTestClusters().isEmpty()
                ? "No new data for the given queries. Showing baseline data if any."
                : "No anomaly found"
        : analysisRecord.getAnalysisSummaryMessage();

    int unknownClusters = 0;
    int highRiskClusters = 0;
    int mediumRiskCluster = 0;
    int lowRiskClusters = 0;
    if (isNotEmpty(analysisSummary.getUnknownClusters())) {
      for (LogMLClusterSummary clusterSummary : analysisSummary.getUnknownClusters()) {
        if (clusterSummary.getScore() > HIGH_RISK_THRESHOLD) {
          ++highRiskClusters;
        } else if (clusterSummary.getScore() > MEDIUM_RISK_THRESHOLD) {
          ++mediumRiskCluster;
        } else if (clusterSummary.getScore() > 0) {
          ++lowRiskClusters;
        }
      }
      riskLevel = highRiskClusters > 0
          ? RiskLevel.HIGH
          : mediumRiskCluster > 0 ? RiskLevel.MEDIUM : lowRiskClusters > 0 ? RiskLevel.LOW : RiskLevel.HIGH;

      unknownClusters = analysisSummary.getUnknownClusters().size();
      analysisSummary.setHighRiskClusters(highRiskClusters);
      analysisSummary.setMediumRiskClusters(mediumRiskCluster);
      analysisSummary.setLowRiskClusters(lowRiskClusters);
    }

    int unknownFrequency = getUnexpectedFrequency(analysisRecord.getTest_clusters());
    if (unknownFrequency > 0) {
      analysisSummary.setHighRiskClusters(analysisSummary.getHighRiskClusters() + unknownFrequency);
      riskLevel = RiskLevel.HIGH;
    }

    if (highRiskClusters > 0 || mediumRiskCluster > 0 || lowRiskClusters > 0) {
      analysisSummaryMsg = analysisSummary.getHighRiskClusters() + " high risk, "
          + analysisSummary.getMediumRiskClusters() + " medium risk, " + analysisSummary.getLowRiskClusters()
          + " low risk anomalous cluster(s) found";
    } else if (unknownClusters > 0 || unknownFrequency > 0) {
      final int totalAnomalies = unknownClusters + unknownFrequency;
      analysisSummaryMsg = totalAnomalies == 1 ? totalAnomalies + " anomalous cluster found"
                                               : totalAnomalies + " anomalous clusters found";
    }

    analysisSummary.setRiskLevel(riskLevel);
    analysisSummary.setAnalysisSummaryMessage(analysisSummaryMsg);
    return analysisSummary;
  }

  @Override
  public void validateConfig(final SettingAttribute settingAttribute, StateType stateType) {
    ErrorCode errorCode = null;
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    try {
      switch (stateType) {
        case SPLUNKV2:
          errorCode = ErrorCode.SPLUNK_CONFIGURATION_ERROR;
          SyncTaskContext splunkTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(SplunkDelegateService.class, splunkTaskContext)
              .validateConfig((SplunkConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        case ELK:
          errorCode = ErrorCode.ELK_CONFIGURATION_ERROR;
          SyncTaskContext elkTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .validateConfig((ElkConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        case LOGZ:
          errorCode = ErrorCode.LOGZ_CONFIGURATION_ERROR;
          SyncTaskContext logzTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(LogzDelegateService.class, logzTaskContext)
              .validateConfig((LogzConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        case SUMO:
          errorCode = ErrorCode.SUMO_CONFIGURATION_ERROR;
          SyncTaskContext sumoTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(SumoDelegateService.class, sumoTaskContext)
              .validateConfig((SumoConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        default:
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          throw new IllegalStateException("Invalid state type: " + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode, e).addParam("reason", e.getMessage());
    }
  }

  @Override
  public Object getLogSample(String accountId, String analysisServerConfigId, String index, StateType stateType) {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No " + stateType + " setting with id: " + analysisServerConfigId + " found");
    }
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
    ErrorCode errorCode = null;
    try {
      switch (stateType) {
        case ELK:
          errorCode = ErrorCode.ELK_CONFIGURATION_ERROR;
          SyncTaskContext elkTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
          return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .getLogSample((ElkConfig) settingAttribute.getValue(), index, encryptedDataDetails);
        case LOGZ:
          errorCode = ErrorCode.LOGZ_CONFIGURATION_ERROR;
          SyncTaskContext logzTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          return delegateProxyFactory.get(LogzDelegateService.class, logzTaskContext)
              .getLogSample((LogzConfig) settingAttribute.getValue(), encryptedDataDetails);
        default:
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          throw new IllegalStateException("Invalid state type: " + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode).addParam("reason", e.getMessage());
    }
  }

  private List<LogMLClusterSummary> computeCluster(Map<String, Map<String, SplunkAnalysisCluster>> cluster,
      Map<String, LogMLScore> clusterScores, CLUSTER_TYPE cluster_type) {
    if (cluster == null) {
      return Collections.emptyList();
    }
    final List<LogMLClusterSummary> analysisSummaries = new ArrayList<>();
    for (Entry<String, Map<String, SplunkAnalysisCluster>> labelEntry : cluster.entrySet()) {
      final LogMLClusterSummary clusterSummary = new LogMLClusterSummary();
      clusterSummary.setHostSummary(new HashMap<>());
      for (Entry<String, SplunkAnalysisCluster> hostEntry : labelEntry.getValue().entrySet()) {
        final LogMLHostSummary hostSummary = new LogMLHostSummary();
        final SplunkAnalysisCluster analysisCluster = hostEntry.getValue();
        hostSummary.setXCordinate(sprinkalizedCordinate(analysisCluster.getX()));
        hostSummary.setYCordinate(sprinkalizedCordinate(analysisCluster.getY()));
        hostSummary.setUnexpectedFreq(analysisCluster.isUnexpected_freq());
        hostSummary.setCount(computeCountFromFrequencies(analysisCluster));
        hostSummary.setFrequencies(getFrequencies(analysisCluster));
        hostSummary.setFrequencyMap(getFrequencyMap(analysisCluster));
        clusterSummary.setLogText(analysisCluster.getText());
        clusterSummary.setTags(analysisCluster.getTags());
        clusterSummary.setClusterLabel(analysisCluster.getCluster_label());
        clusterSummary.getHostSummary().put(Misc.replaceUnicodeWithDot(hostEntry.getKey()), hostSummary);

        double score;
        if (clusterScores != null && clusterScores.containsKey(labelEntry.getKey())) {
          switch (cluster_type) {
            case CONTROL:
              noop();
              break;
            case TEST:
              score = clusterScores.get(labelEntry.getKey()).getFreq_score() * 100;
              clusterSummary.setScore(score);
              clusterSummary.setRiskLevel(RiskLevel.HIGH);
              break;
            case UNKNOWN:
              score = clusterScores.get(labelEntry.getKey()).getTest_score() * 100;
              clusterSummary.setScore(score);
              clusterSummary.setRiskLevel(score > HIGH_RISK_THRESHOLD
                      ? RiskLevel.HIGH
                      : score > MEDIUM_RISK_THRESHOLD ? RiskLevel.MEDIUM : RiskLevel.LOW);
              break;
            default:
              unhandled(cluster_type);
          }
        }
      }
      analysisSummaries.add(clusterSummary);
    }

    return analysisSummaries;
  }

  private Map<Integer, Integer> getFrequencyMap(SplunkAnalysisCluster analysisCluster) {
    Map<Integer, Integer> frequencyMap = new HashMap<>();
    int count;
    for (Map frequency : analysisCluster.getMessage_frequencies()) {
      if (!frequency.containsKey("count")) {
        continue;
      }
      count = (Integer) frequency.get("count");
      if (!frequencyMap.containsKey(count)) {
        frequencyMap.put(count, 0);
      }
      frequencyMap.put(count, frequencyMap.get(count) + 1);
    }
    return frequencyMap;
  }

  private int computeCountFromFrequencies(SplunkAnalysisCluster analysisCluster) {
    int count = 0;
    for (Map frequency : analysisCluster.getMessage_frequencies()) {
      if (!frequency.containsKey("count")) {
        continue;
      }

      count += (Integer) frequency.get("count");
    }

    return count;
  }

  private List<Integer> getFrequencies(SplunkAnalysisCluster analysisCluster) {
    List<Integer> counts = new ArrayList<>();
    for (Map frequency : analysisCluster.getMessage_frequencies()) {
      if (!frequency.containsKey("count")) {
        continue;
      }

      counts.add((Integer) frequency.get("count"));
    }

    return counts;
  }

  private int getUnexpectedFrequency(Map<String, Map<String, SplunkAnalysisCluster>> testClusters) {
    int unexpectedFrequency = 0;
    if (testClusters == null) {
      return unexpectedFrequency;
    }
    for (Entry<String, Map<String, SplunkAnalysisCluster>> labelEntry : testClusters.entrySet()) {
      for (Entry<String, SplunkAnalysisCluster> hostEntry : labelEntry.getValue().entrySet()) {
        final SplunkAnalysisCluster analysisCluster = hostEntry.getValue();
        if (analysisCluster.isUnexpected_freq()) {
          unexpectedFrequency++;
          break;
        }
      }
    }

    return unexpectedFrequency;
  }

  private double sprinkalizedCordinate(double coordinate) {
    final int sprinkleRatio = random.nextInt() % 8;
    double adjustmentBase = coordinate - Math.floor(coordinate);
    return coordinate + (adjustmentBase * sprinkleRatio) / 100;
  }

  @Override
  public boolean purgeLogs() {
    final PageRequest<Workflow> workflowRequest = PageRequest.Builder.aPageRequest().build();
    PageResponse<Workflow> workflows = wingsPersistence.query(Workflow.class, workflowRequest);
    for (Workflow workflow : workflows) {
      final PageRequest<WorkflowExecution> workflowExecutionRequest =
          PageRequest.Builder.aPageRequest()
              .addFilter("workflowId", Operator.EQ, workflow.getUuid())
              .addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS)
              .addOrder("createdAt", OrderType.DESC)
              .build();
      final PageResponse<WorkflowExecution> workflowExecutions =
          workflowExecutionService.listExecutions(workflowExecutionRequest, false, true, false, false);
      for (StateType stateType : logAnalysisStates) {
        purgeLogs(stateType, workflowExecutions);
      }
    }
    return true;
  }

  private void purgeLogs(StateType stateType, PageResponse<WorkflowExecution> workflowExecutions) {
    for (WorkflowExecution workflowExecution : workflowExecutions) {
      if (logExist(stateType, workflowExecution)) {
        deleteNotRequiredLogs(stateType, workflowExecution);
        return;
      }
    }
  }

  private boolean logExist(StateType stateType, WorkflowExecution workflowExecution) {
    Query<LogDataRecord> lastSuccessfulRecords = wingsPersistence.createQuery(LogDataRecord.class)
                                                     .field("stateType")
                                                     .equal(stateType)
                                                     .field("workflowId")
                                                     .equal(workflowExecution.getWorkflowId())
                                                     .field("workflowExecutionId")
                                                     .equal(workflowExecution.getUuid())
                                                     .limit(1);

    return !lastSuccessfulRecords.asList().isEmpty();
  }

  private void deleteNotRequiredLogs(StateType stateType, WorkflowExecution workflowExecution) {
    Query<LogDataRecord> deleteQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                           .field("stateType")
                                           .equal(stateType)
                                           .field("workflowId")
                                           .equal(workflowExecution.getWorkflowId())
                                           .field("workflowExecutionId")
                                           .notEqual(workflowExecution.getUuid());
    logger.info("deleting " + stateType + " logs for workflow:" + workflowExecution.getWorkflowId()
        + " last successful execution: " + workflowExecution.getUuid());
    // wingsPersistence.delete(deleteQuery);
  }

  @Override
  public void createAndSaveSummary(
      StateType stateType, String appId, String stateExecutionId, String query, String message) {
    final LogMLAnalysisRecord analysisRecord = new LogMLAnalysisRecord();
    analysisRecord.setLogCollectionMinute(-1);
    analysisRecord.setStateType(stateType);
    analysisRecord.setApplicationId(appId);
    analysisRecord.setStateExecutionId(stateExecutionId);
    analysisRecord.setQuery(query);
    analysisRecord.setAnalysisSummaryMessage(message);
    analysisRecord.setControl_events(Collections.emptyMap());
    analysisRecord.setTest_events(Collections.emptyMap());
    saveLogAnalysisRecords(analysisRecord, stateType, Optional.empty());
  }

  @Override
  public boolean isStateValid(String appdId, String stateExecutionID) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appdId, stateExecutionID);
    return stateExecutionInstance != null && !stateExecutionInstance.getStatus().isFinalStatus();
  }

  @Override
  public int getCollectionMinuteForLevel(String query, String appdId, String stateExecutionId, StateType type,
      ClusterLevel clusterLevel, Set<String> nodes) {
    ClusterLevel heartBeat = ClusterLevel.getHeartBeatLevel(clusterLevel);

    while (true) {
      /**
       * Get the heartbeat records for L1.
       */
      Iterator<LogDataRecord> logHeartBeatRecordsIterator = wingsPersistence.createQuery(LogDataRecord.class)
                                                                .field("applicationId")
                                                                .equal(appdId)
                                                                .field("stateExecutionId")
                                                                .equal(stateExecutionId)
                                                                .field("stateType")
                                                                .equal(type)
                                                                .field("clusterLevel")
                                                                .equal(heartBeat)
                                                                .field("query")
                                                                .equal(query)
                                                                .field("host")
                                                                .in(nodes)
                                                                .order("logCollectionMinute")
                                                                .fetch();

      if (!logHeartBeatRecordsIterator.hasNext()) {
        return -1;
      }

      int logCollectionMinute = -1;
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
              query, appdId, stateExecutionId, type, hosts, logCollectionMinute, ClusterLevel.L1, heartBeat)) {
        continue;
      }

      Set<String> lookupNodes = new HashSet<>(nodes);

      for (String node : hosts) {
        lookupNodes.remove(node);
      }

      if (!lookupNodes.isEmpty()) {
        logger.warn(
            "Still waiting for data for " + Arrays.toString(lookupNodes.toArray()) + " for " + stateExecutionId);
      }

      return lookupNodes.isEmpty() ? logCollectionMinute : -1;
    }
  }

  @Override
  public boolean hasDataRecords(String query, String appdId, String stateExecutionId, StateType type, Set<String> nodes,
      ClusterLevel level, int logCollectionMinute) {
    /**
     * Get the data records for the found heartbeat.
     */
    Iterator<LogDataRecord> logDataRecordsIterator = wingsPersistence.createQuery(LogDataRecord.class)
                                                         .field("applicationId")
                                                         .equal(appdId)
                                                         .field("stateExecutionId")
                                                         .equal(stateExecutionId)
                                                         .field("stateType")
                                                         .equal(type)
                                                         .field("clusterLevel")
                                                         .equal(level)
                                                         .field("logCollectionMinute")
                                                         .equal(logCollectionMinute)
                                                         .field("query")
                                                         .equal(query)
                                                         .field("host")
                                                         .in(nodes)
                                                         .fetch(new FindOptions().limit(1));

    return logDataRecordsIterator.hasNext();
  }

  @Override
  public Optional<LogDataRecord> getHearbeatRecordForL0(
      String appId, String stateExecutionId, StateType type, String host) {
    /**
     * Find heartbeat for L0 records. L0 heartbeat is H0.
     */
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class)
                                     .field("applicationId")
                                     .equal(appId)
                                     .field("stateExecutionId")
                                     .equal(stateExecutionId)
                                     .field("stateType")
                                     .equal(type)
                                     .field("clusterLevel")
                                     .equal(ClusterLevel.getHeartBeatLevel(ClusterLevel.L0))
                                     .order("logCollectionMinute");

    if (isNotEmpty(host)) {
      query = query.field("host").equal(host);
    }

    Iterator<LogDataRecord> logDataRecordsIterator = query.fetch(new FindOptions().limit(1));
    // Nothing more to process. break.
    if (!logDataRecordsIterator.hasNext()) {
      return Optional.empty();
    }

    return Optional.of(logDataRecordsIterator.next());
  }

  private int getLastProcessedMinute(String query, String appId, String stateExecutionId, StateType type) {
    Iterator<LogDataRecord> logDataRecordsIterator = wingsPersistence.createQuery(LogDataRecord.class)
                                                         .field("applicationId")
                                                         .equal(appId)
                                                         .field("stateExecutionId")
                                                         .equal(stateExecutionId)
                                                         .field("stateType")
                                                         .equal(type)
                                                         .field("clusterLevel")
                                                         .equal(ClusterLevel.getFinal())
                                                         .field("query")
                                                         .equal(query)
                                                         .order("-logCollectionMinute")
                                                         .fetch(new FindOptions().limit(1));

    return logDataRecordsIterator.hasNext() ? logDataRecordsIterator.next().getLogCollectionMinute() : -1;
  }

  @Override
  public boolean isProcessingComplete(
      String query, String appId, String stateExecutionId, StateType type, int timeDurationMins) {
    return getLastProcessedMinute(query, appId, stateExecutionId, type) >= timeDurationMins - 1;
  }

  private boolean deleteIfStale(String query, String appId, String stateExecutionId, StateType type, Set<String> hosts,
      int logCollectionMinute, ClusterLevel clusterLevel, ClusterLevel heartBeat) {
    int lastProcessedMinute = getLastProcessedMinute(query, appId, stateExecutionId, type);
    if (logCollectionMinute <= lastProcessedMinute) {
      logger.info("deleting stale data for stateExecutionID = " + stateExecutionId + " logCollectionMinute "
          + logCollectionMinute);
      deleteClusterLevel(type, stateExecutionId, appId, query, hosts, logCollectionMinute, clusterLevel, heartBeat);
      return true;
    }
    return false;
  }

  public enum CLUSTER_TYPE { CONTROL, TEST, UNKNOWN }
  public enum LogMLFeedbackType {
    IGNORE_SERVICE,
    IGNORE_WORKFLOW,
    IGNORE_WORKFLOW_EXECUTION,
    IGNORE_ALWAYS,
    DISMISS,
    PRIORITIZE,
    THUMBS_UP,
    THUMBS_DOWN
  }
}
