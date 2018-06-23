package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.codec.digest.DigestUtils;
import org.mongodb.morphia.query.CountOptions;
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
import software.wings.dl.HIterator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
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

  @SuppressFBWarnings("MS_PKGPROTECT")
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
                                     .filter("stateType", stateType)
                                     .filter("stateExecutionId", stateExecutionId)
                                     .filter("appId", appId)
                                     .filter("query", searchQuery)
                                     .filter("logCollectionMinute", logCollectionMinute)
                                     .filter("clusterLevel", fromLevel);

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

  @Override
  public void cleanUpForLogRetry(String stateExecutionId) {
    // delete log data records
    wingsPersistence.delete(
        wingsPersistence.createQuery(LogDataRecord.class).filter("stateExecutionId", stateExecutionId));

    // delete log analysis records
    wingsPersistence.delete(
        wingsPersistence.createQuery(LogMLAnalysisRecord.class).filter("stateExecutionId", stateExecutionId));

    // delete cv dashboard execution data
    wingsPersistence.delete(wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
                                .filter("stateExecutionId", stateExecutionId));

    // delete learning engine tasks
    wingsPersistence.delete(
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).filter("state_execution_id", stateExecutionId));

    // delete experimental learning engine tasks
    wingsPersistence.delete(wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class)
                                .filter("state_execution_id", stateExecutionId));

    // delete experimental log analysis records
    wingsPersistence.delete(wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class)
                                .filter("stateExecutionId", stateExecutionId));
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

  @Override
  public List<LogDataRecord> getLogData(LogRequest logRequest, boolean compareCurrent, String workflowExecutionId,
      ClusterLevel clusterLevel, StateType stateType) {
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
      recordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                        .filter("stateType", stateType)
                        .filter("workflowExecutionId", workflowExecutionId)
                        .filter("appId", logRequest.getApplicationId())
                        .filter("query", logRequest.getQuery())
                        .filter("serviceId", logRequest.getServiceId())
                        .filter("clusterLevel", clusterLevel)
                        .filter("logCollectionMinute", logRequest.getLogCollectionMinute());
    }

    List<LogDataRecord> rv = new ArrayList<>();
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

  private boolean deleteFeedbackHelper(String feedbackId) {
    Query<LogMLFeedbackRecord> query =
        wingsPersistence.createQuery(LogMLFeedbackRecord.class).filter("_id", feedbackId);

    wingsPersistence.delete(query);
    return true;
  }

  @Override
  public boolean deleteFeedback(String feedbackId) {
    if (isEmpty(feedbackId)) {
      throw new WingsException("empty or null feedback id set ");
    }

    return deleteFeedbackHelper(feedbackId);
  }

  @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
  @Override
  public LogMLAnalysisSummary getAnalysisSummaryForDemo(
      String stateExecutionId, String applicationId, StateType stateType) {
    logger.info("Creating log analysis summary for demo {}", stateExecutionId);
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(applicationId, stateExecutionId);
    if (stateExecutionId == null) {
      logger.error("State execution instance not found for {}", stateExecutionId);
      return null;
    }

    SettingAttribute settingAttribute = settingsService.get(
        ((LogAnalysisExecutionData) stateExecutionInstance.getStateExecutionData()).getServerConfigId());

    if (stateType.equals(StateType.ELK)) {
      if (settingAttribute.getName().toLowerCase().endsWith("dev")
          || settingAttribute.getName().toLowerCase().endsWith("prod")) {
        if (stateExecutionInstance.getStatus() == ExecutionStatus.SUCCESS) {
          return getAnalysisSummary("CV-Demo-LOG-Success", "CV-Demo", stateType);
        } else {
          return getAnalysisSummary("CV-Demo-LOG-Failure", "CV-Demo", stateType);
        }
      }
    }

    return getAnalysisSummary(stateExecutionId, applicationId, stateType);
  }

  @Override
  public List<LogMLFeedbackRecord> getMLFeedback(String serviceId, String workflowId, String workflowExecutionId) {
    Query<LogMLFeedbackRecord> query = wingsPersistence.createQuery(LogMLFeedbackRecord.class);
    query.or(query.criteria("serviceId").equal(serviceId), query.criteria("workflowId").equal(workflowId),
        query.criteria("workflowExecutionId").equal(workflowExecutionId));

    return query.asList();
  }

  @SuppressFBWarnings({"NP_ALWAYS_NULL", "NP_LOAD_OF_KNOWN_NULL_VALUE"})
  @Override
  public boolean saveFeedback(LogMLFeedback feedback, StateType stateType) {
    if (!isEmpty(feedback.getLogMLFeedbackId())) {
      deleteFeedbackHelper(feedback.getLogMLFeedbackId());
    }

    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, feedback.getAppId(), feedback.getStateExecutionId());

    if (stateExecutionInstance == null) {
      throw new WingsException("Unable to find state execution for id " + stateExecutionInstance.getUuid());
    }

    LogMLAnalysisSummary analysisSummary =
        getAnalysisSummary(feedback.getStateExecutionId(), feedback.getAppId(), stateType);

    if (analysisSummary == null) {
      throw new WingsException("Unable to find analysisSummary for feedback " + feedback);
    }

    String logText = "";
    List<LogMLClusterSummary> logMLClusterSummaryList;
    switch (feedback.getClusterType()) {
      case CONTROL:
        logMLClusterSummaryList = analysisSummary.getControlClusters();
        break;
      case TEST:
        logMLClusterSummaryList = analysisSummary.getTestClusters();
        break;
      case UNKNOWN:
        logMLClusterSummaryList = analysisSummary.getUnknownClusters();
        break;
      default:
        throw new WingsException("unsupported cluster type " + feedback.getClusterType() + " in feedback");
    }

    for (LogMLClusterSummary clusterSummary : logMLClusterSummaryList) {
      if (clusterSummary.getClusterLabel() == feedback.getClusterLabel()) {
        logText = clusterSummary.getLogText();
      }
    }

    if (isEmpty(logText)) {
      throw new WingsException("Unable to find logText for feedback " + feedback);
    }

    String logmd5Hash = DigestUtils.md5Hex(logText);

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
                                               .appId(feedback.getAppId())
                                               .serviceId(phaseElement.getServiceElement().getUuid())
                                               .workflowId(stateExecutionInstance.getWorkflowId())
                                               .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
                                               .stateExecutionId(feedback.getStateExecutionId())
                                               .logMessage(logText)
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
      String appId, String stateExecutionId, String query, int logCollectionMinute, StateType stateType) {
    Query<LogDataRecord> splunkLogDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                                        .filter("stateType", stateType)
                                                        .filter("stateExecutionId", stateExecutionId)
                                                        .filter("appId", appId)
                                                        .filter("query", query)
                                                        .filter("logCollectionMinute", logCollectionMinute);
    return !splunkLogDataRecordQuery.asList().isEmpty();
  }

  @Override
  public boolean isBaselineCreated(AnalysisComparisonStrategy comparisonStrategy, StateType stateType, String appId,
      String workflowId, String workflowExecutionId, String serviceId) {
    if (comparisonStrategy == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
      return true;
    }
    return getLastSuccessfulWorkflowExecutionIdWithLogs(stateType, appId, serviceId, workflowId) != null;
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithLogs(
      StateType stateType, String appId, String serviceId, String workflowId) {
    // TODO should we limit the number of executions to search in ??
    List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(appId, workflowId);
    for (String successfulExecution : successfulExecutions) {
      if (wingsPersistence.createQuery(LogDataRecord.class)
              .filter("appId", appId)
              .filter("stateType", stateType)
              .filter("workflowId", workflowId)
              .filter("workflowExecutionId", successfulExecution)
              .filter("serviceId", serviceId)
              .filter("clusterLevel", ClusterLevel.L2)
              .count(new CountOptions().limit(1))
          > 0) {
        return successfulExecution;
      }
    }
    logger.warn("Could not get a successful workflow to find control nodes");
    return null;
  }

  private List<String> getLastSuccessfulWorkflowExecutionIds(String appId, String workflowId) {
    final PageRequest<WorkflowExecution> pageRequest = aPageRequest()
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
      mlAnalysisResponse.setControl_clusters(getClustersWithDotsReplaced(mlAnalysisResponse.getControl_clusters()));
    }

    // replace dots in test cluster
    if (mlAnalysisResponse.getTest_clusters() != null) {
      mlAnalysisResponse.setTest_clusters(getClustersWithDotsReplaced(mlAnalysisResponse.getTest_clusters()));
    }

    // replace dots in unknown cluster
    if (mlAnalysisResponse.getUnknown_clusters() != null) {
      mlAnalysisResponse.setUnknown_clusters(getClustersWithDotsReplaced(mlAnalysisResponse.getUnknown_clusters()));
    }

    // replace dots in ignored cluster
    if (mlAnalysisResponse.getIgnore_clusters() != null) {
      mlAnalysisResponse.setIgnore_clusters(getClustersWithDotsReplaced(mlAnalysisResponse.getIgnore_clusters()));
    }

    if (mlAnalysisResponse.getLogCollectionMinute() == -1 || !isEmpty(mlAnalysisResponse.getControl_events())
        || !isEmpty(mlAnalysisResponse.getTest_events())) {
      wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(mlAnalysisResponse));
    }
    if (logger.isDebugEnabled()) {
      logger.debug("inserted ml LogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getAppId()
          + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
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
    if (logger.isDebugEnabled()) {
      logger.debug("inserted ml LogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getAppId()
          + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
    }
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
      String appId, String stateExecutionId, String query, StateType stateType, Integer logCollectionMinute) {
    return wingsPersistence.createQuery(LogMLAnalysisRecord.class)
        .filter("stateExecutionId", stateExecutionId)
        .filter("appId", appId)
        .filter("query", query)
        .filter("stateType", stateType)
        .field("logCollectionMinute")
        .lessThanOrEq(logCollectionMinute)
        .order("-logCollectionMinute")
        .get();
  }

  @Override
  public LogMLAnalysisSummary getExperimentalAnalysisSummary(
      String stateExecutionId, String appId, StateType stateType, String expName) {
    ExperimentalLogMLAnalysisRecord analysisRecord = wingsPersistence.createQuery(ExperimentalLogMLAnalysisRecord.class)
                                                         .filter("stateExecutionId", stateExecutionId)
                                                         .filter("appId", appId)
                                                         .filter("stateType", stateType)
                                                         .filter("experiment_name", expName)
                                                         .order("-logCollectionMinute")
                                                         .get();
    if (analysisRecord == null) {
      return null;
    }
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

    RiskLevel riskLevel = RiskLevel.NA;
    String analysisSummaryMsg = isEmpty(analysisRecord.getAnalysisSummaryMessage())
        ? analysisSummary.getControlClusters().isEmpty()
            ? "No baseline data for the given queries. This will be baseline for the next run."
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

  private Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> getMLUserFeedbacks(
      String stateExecutionId, String appId) {
    Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> userFeedbackMap = new HashMap<>();
    userFeedbackMap.put(CLUSTER_TYPE.CONTROL, new HashMap<>());
    userFeedbackMap.put(CLUSTER_TYPE.TEST, new HashMap<>());
    userFeedbackMap.put(CLUSTER_TYPE.UNKNOWN, new HashMap<>());

    List<LogMLFeedbackRecord> logMLFeedbackRecords = wingsPersistence.createQuery(LogMLFeedbackRecord.class)
                                                         .filter("stateExecutionId", stateExecutionId)
                                                         .filter("appId", appId)
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
  public LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String appId, StateType stateType) {
    LogMLAnalysisRecord analysisRecord = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                             .filter("stateExecutionId", stateExecutionId)
                                             .filter("appId", appId)
                                             .filter("stateType", stateType)
                                             .order("-logCollectionMinute")
                                             .get();

    if (analysisRecord == null) {
      return null;
    }

    Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> mlUserFeedbacks = getMLUserFeedbacks(stateExecutionId, appId);

    final LogMLAnalysisSummary analysisSummary = new LogMLAnalysisSummary();
    analysisSummary.setQuery(analysisRecord.getQuery());
    analysisSummary.setScore(analysisRecord.getScore() * 100);
    analysisSummary.setBaseLineExecutionId(analysisRecord.getBaseLineExecutionId());
    analysisSummary.setControlClusters(
        computeCluster(analysisRecord.getControl_clusters(), Collections.emptyMap(), CLUSTER_TYPE.CONTROL));
    LogMLClusterScores logMLClusterScores =
        analysisRecord.getCluster_scores() != null ? analysisRecord.getCluster_scores() : new LogMLClusterScores();
    analysisSummary.setTestClusters(
        computeCluster(analysisRecord.getTest_clusters(), logMLClusterScores.getTest(), CLUSTER_TYPE.TEST));
    analysisSummary.setUnknownClusters(
        computeCluster(analysisRecord.getUnknown_clusters(), logMLClusterScores.getUnknown(), CLUSTER_TYPE.UNKNOWN));
    analysisSummary.setIgnoreClusters(
        computeCluster(analysisRecord.getIgnore_clusters(), Collections.emptyMap(), CLUSTER_TYPE.IGNORE));

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
    analysisSummary.setStateType(stateType);
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
      throw new WingsException(errorCode, e).addParam("reason", Misc.getMessage(e));
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
      throw new WingsException(errorCode).addParam("reason", Misc.getMessage(e));
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
    if (isEmpty(analysisCluster.getMessage_frequencies())) {
      return frequencyMap;
    }
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
    if (isEmpty(analysisCluster.getMessage_frequencies())) {
      return count;
    }
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
    if (isEmpty(analysisCluster.getMessage_frequencies())) {
      return counts;
    }
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
    if (isEmpty(testClusters)) {
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
    final PageRequest<Workflow> workflowRequest = aPageRequest().build();
    PageResponse<Workflow> workflows = wingsPersistence.query(Workflow.class, workflowRequest);
    for (Workflow workflow : workflows) {
      final PageRequest<WorkflowExecution> workflowExecutionRequest =
          aPageRequest()
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
    return wingsPersistence.createQuery(LogDataRecord.class)
               .filter("appId", workflowExecution.getAppId())
               .filter("stateType", stateType)
               .filter("workflowId", workflowExecution.getWorkflowId())
               .filter("workflowExecutionId", workflowExecution.getUuid())
               .count(new CountOptions().limit(1))
        > 0;
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  private void deleteNotRequiredLogs(StateType stateType, WorkflowExecution workflowExecution) {
    Query<LogDataRecord> deleteQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                           .filter("appId", workflowExecution.getAppId())
                                           .filter("stateType", stateType)
                                           .filter("workflowId", workflowExecution.getWorkflowId())
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
    analysisRecord.setAppId(appId);
    analysisRecord.setStateExecutionId(stateExecutionId);
    analysisRecord.setQuery(query);
    analysisRecord.setAnalysisSummaryMessage(message);
    analysisRecord.setControl_events(Collections.emptyMap());
    analysisRecord.setTest_events(Collections.emptyMap());
    saveLogAnalysisRecords(analysisRecord, stateType, Optional.empty());
  }

  @Override
  public boolean isStateValid(String appId, String stateExecutionID) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appId, stateExecutionID);
    return stateExecutionInstance != null && !ExecutionStatus.isFinalStatus(stateExecutionInstance.getStatus());
  }

  @Override
  public int getCollectionMinuteForLevel(String query, String appId, String stateExecutionId, StateType type,
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
      ClusterLevel level, int logCollectionMinute) {
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

  private int getLastProcessedMinute(String query, String appId, String stateExecutionId, StateType type) {
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

  public enum CLUSTER_TYPE { CONTROL, TEST, UNKNOWN, IGNORE }

  public enum LogMLFeedbackType {
    IGNORE_SERVICE,
    IGNORE_WORKFLOW,
    IGNORE_WORKFLOW_EXECUTION,
    IGNORE_ALWAYS,
    DISMISS,
    PRIORITIZE,
    THUMBS_UP,
    THUMBS_DOWN,
    UNDO_IGNORE
  }
}
