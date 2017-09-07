package software.wings.service.impl.analysis;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.SplunkConfig;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.config.LogzConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.AbstractAnalysisState;
import software.wings.sm.states.AbstractLogAnalysisState;

import java.io.IOException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
public class AnalysisServiceImpl implements AnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);
  private static int NUM_OF_FIRSTL_LEVEL_CLUSTERING_THREADS =
      StringUtils.isBlank(System.getProperty("clustering.threads"))
      ? 10
      : Integer.parseInt(System.getProperty("clustering.threads"));
  private final Random random = new Random();

  public static final StateType[] logAnalysisStates = new StateType[] {StateType.SPLUNKV2, StateType.ELK};

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject protected SettingsService settingsService;
  @Inject protected WorkflowExecutionService workflowExecutionService;
  @Inject protected MainConfiguration configuration;
  @Inject protected DelegateServiceImpl delegateService;

  private ExecutorService firstLevelClusteringService =
      Executors.newFixedThreadPool(NUM_OF_FIRSTL_LEVEL_CLUSTERING_THREADS, r -> new Thread(r, "clustering_thread"));

  @Override
  public void bumpClusterLevel(String stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, int logCollectionMinute, ClusterLevel fromLevel, ClusterLevel toLevel) {
    Query<LogDataRecord> query = wingsPersistence.createQuery(LogDataRecord.class)
                                     .field("stateType")
                                     .equal(StateType.valueOf(stateType))
                                     .field("stateExecutionId")
                                     .equal(stateExecutionId)
                                     .field(("applicationId"))
                                     .equal(appId)
                                     .field("query")
                                     .equal(searchQuery)
                                     .field("host")
                                     .in(host)
                                     .field("logCollectionMinute")
                                     .equal(logCollectionMinute)
                                     .field("clusterLevel")
                                     .equal(fromLevel);
    wingsPersistence.update(
        query, wingsPersistence.createUpdateOperations(LogDataRecord.class).set("clusterLevel", toLevel));
  }

  @Override
  public void deleteClusterLevel(String stateType, String stateExecutionId, String appId, String searchQuery,
      Set<String> host, int logCollectionMinute, ClusterLevel... clusterLevels) {
    Query<LogDataRecord> records = wingsPersistence.createQuery(LogDataRecord.class)
                                       .field("stateType")
                                       .equal(StateType.valueOf(stateType))
                                       .field("stateExecutionId")
                                       .equal(stateExecutionId)
                                       .field(("applicationId"))
                                       .equal(appId)
                                       .field("query")
                                       .equal(searchQuery)
                                       .field("host")
                                       .in(host)
                                       .field("logCollectionMinute")
                                       .equal(logCollectionMinute)
                                       .field("clusterLevel")
                                       .in(Arrays.asList(clusterLevels));
    wingsPersistence.delete(records);
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

      if (logData.size() == 0) {
        return true;
      }

      boolean hasHeartBeat = Integer.parseInt(logData.get(0).getClusterLabel()) < 0;

      if (clusterLevel == ClusterLevel.L0 && !hasHeartBeat) {
        logger.error("Delegate reporting log records without a "
            + "heartbeat for state " + stateType + " : id " + stateExecutionId);
        return false;
      }

      if (stateType == StateType.SPLUNKV2 && clusterLevel == ClusterLevel.L1 && !hasHeartBeat) {
        logger.error("Delegate reporting log records without a "
            + "heartbeat for state " + stateType + " : id " + stateExecutionId);

        return false;
      }

      List<LogDataRecord> logDataRecords =
          LogDataRecord.generateDataRecords(stateType, appId, stateExecutionId, workflowId, workflowExecutionId,
              serviceId, clusterLevel, ClusterLevel.getHeartBeatLevel(clusterLevel), logData);
      wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);
      logger.info("inserted " + logDataRecords.size() + " LogDataRecord to persistence layer.");

      return true;
    } catch (Exception ex) {
      logger.error("Save log data failed " + ex);
      return false;
    }
  }

  @Override
  public List<LogDataRecord> getLogData(
      LogRequest logRequest, boolean compareCurrent, ClusterLevel clusterLevel, StateType stateType) {
    Query<LogDataRecord> splunkLogDataRecordQuery = null;
    List<LogDataRecord> records = null;
    if (compareCurrent) {
      splunkLogDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                     .field("stateType")
                                     .equal(stateType)
                                     .field("stateExecutionId")
                                     .equal(logRequest.getStateExecutionId())
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
                                     .hasAnyOf(logRequest.getNodes());
    } else {
      final String lastSuccessfulWorkflowExecutionId = getLastSuccessfulWorkflowExecutionIdWithLogs(stateType,
          logRequest.getApplicationId(), logRequest.getServiceId(), logRequest.getQuery(), logRequest.getWorkflowId());
      Preconditions.checkNotNull(lastSuccessfulWorkflowExecutionId,
          "No successful workflow execution found for workflowId: " + logRequest.getWorkflowId());

      splunkLogDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class)
                                     .field("stateType")
                                     .equal(stateType)
                                     .field("serviceId")
                                     .equal(logRequest.getServiceId())
                                     .field("workflowId")
                                     .equal(logRequest.getWorkflowId())
                                     .field("workflowExecutionId")
                                     .equal(lastSuccessfulWorkflowExecutionId)
                                     .field("applicationId")
                                     .equal(logRequest.getApplicationId())
                                     .field("query")
                                     .equal(logRequest.getQuery())
                                     .field("host")
                                     .hasAnyOf(logRequest.getNodes())
                                     .field("clusterLevel")
                                     .equal(clusterLevel)
                                     .field("logCollectionMinute")
                                     .equal(logRequest.getLogCollectionMinute());
    }

    records = splunkLogDataRecordQuery.asList();
    logger.debug("returning " + records.size() + " records for request: " + logRequest);
    return records;
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
    return splunkLogDataRecordQuery.asList().size() > 0;
  }

  @Override
  public boolean isBaselineCreated(AnalysisComparisonStrategy comparisonStrategy, StateType stateType,
      String applicationId, String workflowId, String workflowExecutionId, String serviceId, String query) {
    if (comparisonStrategy == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
      return true;
    }
    final List<String> successfulExecutions = getLastSuccessfulWorkflowExecutionIds(applicationId, workflowId);
    if (successfulExecutions.isEmpty()) {
      return false;
    }

    Query<LogDataRecord> lastSuccessfulRecords = wingsPersistence.createQuery(LogDataRecord.class)
                                                     .field("stateType")
                                                     .equal(stateType)
                                                     .field("workflowId")
                                                     .equal(workflowId)
                                                     .field("workflowExecutionId")
                                                     .hasAnyOf(successfulExecutions)
                                                     .field("serviceId")
                                                     .equal(serviceId)
                                                     .field("query")
                                                     .equal(query)
                                                     .limit(1);

    return lastSuccessfulRecords.asList().size() > 0;
  }

  private String getLastSuccessfulWorkflowExecutionIdWithLogs(
      StateType stateType, String appId, String serviceId, String query, String workflowId) {
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
                                                           .limit(1);

      List<LogDataRecord> lastSuccessfulRecords = lastSuccessfulRecordQuery.asList();
      if (lastSuccessfulRecords != null && lastSuccessfulRecords.size() > 0) {
        return successfulExecution;
      }
    }
    logger.error("Could not get a successful workflow to find control nodes");
    return null;
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
  public Boolean saveLogAnalysisRecords(LogMLAnalysisRecord mlAnalysisResponse, StateType stateType) {
    mlAnalysisResponse.setStateType(stateType);
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(mlAnalysisResponse));
    logger.debug(
        "inserted ml LogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getApplicationId()
        + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
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

  @Override
  public int getLastAnalysisMinute(String stateExecutionId, String applicationId, StateType stateType) {
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
      return -1;
    }

    return iteratorAnalysisRecord.next().getLogCollectionMinute();
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
    LogMLAnalysisRecord analysisRecord = iteratorAnalysisRecord.next();
    final LogMLAnalysisSummary analysisSummary = new LogMLAnalysisSummary();
    analysisSummary.setQuery(analysisRecord.getQuery());
    analysisSummary.setControlClusters(computeCluster(analysisRecord.getControl_clusters()));
    analysisSummary.setTestClusters(computeCluster(analysisRecord.getTest_clusters()));
    analysisSummary.setUnknownClusters(computeCluster(analysisRecord.getUnknown_clusters()));

    RiskLevel riskLevel = RiskLevel.LOW;
    String analysisSummaryMsg =
        analysisRecord.getAnalysisSummaryMessage() == null || analysisRecord.getAnalysisSummaryMessage().isEmpty()
        ? "No anomaly found"
        : analysisRecord.getAnalysisSummaryMessage();

    int unknownClusters = 0;
    if (analysisSummary.getUnknownClusters() != null && analysisSummary.getUnknownClusters().size() > 0) {
      riskLevel = RiskLevel.HIGH;
      unknownClusters = analysisSummary.getUnknownClusters().size();
    }

    int unknownFrequency = getUnexpectedFrequency(analysisRecord.getTest_clusters());
    if (unknownFrequency > 0) {
      riskLevel = RiskLevel.HIGH;
    }

    if (unknownClusters > 0 || unknownFrequency > 0) {
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
    try {
      switch (stateType) {
        case SPLUNKV2:
          errorCode = ErrorCode.SPLUNK_CONFIGURATION_ERROR;
          SyncTaskContext splunkTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(SplunkDelegateService.class, splunkTaskContext)
              .validateConfig((SplunkConfig) settingAttribute.getValue());
          break;
        case ELK:
          errorCode = ErrorCode.ELK_CONFIGURATION_ERROR;
          SyncTaskContext elkTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .validateConfig((ElkConfig) settingAttribute.getValue());
          break;
        case LOGZ:
          errorCode = ErrorCode.LOGZ_CONFIGURATION_ERROR;
          SyncTaskContext logzTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          delegateProxyFactory.get(LogzDelegateService.class, logzTaskContext)
              .validateConfig((LogzConfig) settingAttribute.getValue());
          break;
        default:
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          throw new IllegalStateException("Invalid state type: " + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode, "reason", e.getMessage());
    }
  }

  @Override
  public Object getLogSample(String accountId, String analysisServerConfigId, String index, StateType stateType) {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No " + stateType + " setting with id: " + analysisServerConfigId + " found");
    }
    ErrorCode errorCode = null;
    try {
      switch (stateType) {
        case ELK:
          errorCode = ErrorCode.ELK_CONFIGURATION_ERROR;
          SyncTaskContext elkTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
          return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .getLogSample((ElkConfig) settingAttribute.getValue(), index);
        case LOGZ:
          errorCode = ErrorCode.LOGZ_CONFIGURATION_ERROR;
          SyncTaskContext logzTaskContext =
              aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
          return delegateProxyFactory.get(LogzDelegateService.class, logzTaskContext)
              .getLogSample((LogzConfig) settingAttribute.getValue());
        default:
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          throw new IllegalStateException("Invalid state type: " + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode, "reason", e.getMessage());
    }
  }

  private List<LogMLClusterSummary> computeCluster(Map<String, Map<String, SplunkAnalysisCluster>> cluster) {
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
        hostSummary.setUnexpectedFreq((analysisCluster.isUnexpected_freq()));
        hostSummary.setCount(computeCountFromFrequencies(analysisCluster));
        clusterSummary.setLogText(analysisCluster.getText());
        clusterSummary.setTags(analysisCluster.getTags());
        clusterSummary.getHostSummary().put(hostEntry.getKey(), hostSummary);
      }
      analysisSummaries.add(clusterSummary);
    }

    return analysisSummaries;
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

    return lastSuccessfulRecords.asList().size() > 0;
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
      String stateType, String appId, String stateExecutionId, String query, String message) {
    final LogMLAnalysisRecord analysisRecord = new LogMLAnalysisRecord();
    analysisRecord.setLogCollectionMinute(-1);
    analysisRecord.setStateType(StateType.valueOf(stateType));
    analysisRecord.setApplicationId(appId);
    analysisRecord.setStateExecutionId(stateExecutionId);
    analysisRecord.setQuery(query);
    analysisRecord.setAnalysisSummaryMessage(message);
    analysisRecord.setControl_events(Collections.emptyMap());
    analysisRecord.setTest_events(Collections.emptyMap());
    saveLogAnalysisRecords(analysisRecord, StateType.valueOf(stateType));
  }

  @Override
  public boolean isStateValid(String appdId, String stateExecutionID) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appdId, stateExecutionID);
    return (stateExecutionInstance == null || stateExecutionInstance.getStatus().isFinalStatus()) ? false : true;
  }

  @Override
  public int getCollectionMinuteForL1(
      String query, String appdId, String stateExecutionId, String type, Set<String> testNodes) {
    ClusterLevel heartBeat = ClusterLevel.getHeartBeatLevel(ClusterLevel.L1);

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
                                                                .equal(StateType.valueOf(type))
                                                                .field("clusterLevel")
                                                                .equal(heartBeat)
                                                                .field("query")
                                                                .equal(query)
                                                                .order("logCollectionMinute")
                                                                .fetch(new FindOptions().limit(testNodes.size()));

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
          hosts.add(logHeartBeatRecordsIterator.next().getHost());
        }
      }

      if (deleteIfStale(
              query, appdId, stateExecutionId, type, hosts, logCollectionMinute, ClusterLevel.L1, heartBeat)) {
        continue;
      }

      Set<String> nodes = new HashSet<>(hosts);

      for (String node : testNodes) {
        nodes.remove(node);
      }

      return nodes.isEmpty() ? logCollectionMinute : -1;

      // logCollectionMinute =  nodes.isEmpty() ? logCollectionMinute : -1;

      //      /**
      //       * Get the data records for the found heartbeat.
      //       */
      //      Iterator<LogDataRecord> logDataRecordsIterator =
      //          wingsPersistence.createQuery(LogDataRecord.class)
      //              .field("applicationId").equal(appdId)
      //              .field("stateExecutionId").equal(stateExecutionId)
      //              .field("stateType").equal(StateType.valueOf(type))
      //              .field("clusterLevel").equal(ClusterLevel.L1)
      //              .field("logCollectionMinute").equal(logCollectionMinute)
      //              .field("query").equal(query)
      //              .field("host").in(hosts)
      //              .fetch(new FindOptions().limit(1));
      //
      //      /**
      //       * Heartbeat has no associated log records. Mark heartbeat as processed.
      //       * Move on.
      //       */
      //      if(!logDataRecordsIterator.hasNext()) {
      //        // TODO - we assume that L1 => L2 and analysis happen synchronously, which is
      //        // TODO - why bump the heartbeat to final.
      //        bumpClusterLevel(type, stateExecutionId, appdId,
      //            query,  hosts, logCollectionMinute, heartBeat, ClusterLevel.getFinal());
      //        continue;
      //      }
      //
      //      return logCollectionMinute;
    }
  }

  @Override
  public boolean hasDataRecords(String query, String appdId, String stateExecutionId, String type, Set<String> nodes,
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
                                                         .equal(StateType.valueOf(type))
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
  public Optional<LogDataRecord> getLogDataRecordForL0(String appId, String stateExecutionId, String type) {
    while (true) {
      /**
       * Find heartbeat for L0 records. L0 heartbeat is H0.
       */
      Iterator<LogDataRecord> logDataRecordsIterator = wingsPersistence.createQuery(LogDataRecord.class)
                                                           .field("applicationId")
                                                           .equal(appId)
                                                           .field("stateExecutionId")
                                                           .equal(stateExecutionId)
                                                           .field("stateType")
                                                           .equal(StateType.valueOf(type))
                                                           .field("clusterLevel")
                                                           .equal(ClusterLevel.getHeartBeatLevel(ClusterLevel.L0))
                                                           .order("logCollectionMinute")
                                                           .fetch(new FindOptions().limit(1));

      // Nothing more to process. break.
      if (!logDataRecordsIterator.hasNext()) {
        return Optional.empty();
      }

      return Optional.of(logDataRecordsIterator.next());
    }
  }

  private int getLastProcessedMinute(String query, String appId, String stateExecutionId, String type) {
    Iterator<LogDataRecord> logDataRecordsIterator = wingsPersistence.createQuery(LogDataRecord.class)
                                                         .field("applicationId")
                                                         .equal(appId)
                                                         .field("stateExecutionId")
                                                         .equal(stateExecutionId)
                                                         .field("stateType")
                                                         .equal(StateType.valueOf(type))
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
      String query, String appId, String stateExecutionId, String type, int timeDurationMins) {
    return getLastProcessedMinute(query, appId, stateExecutionId, type) >= timeDurationMins - 1;
  }

  private boolean deleteIfStale(String query, String appId, String stateExecutionId, String type, Set<String> hosts,
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
}
