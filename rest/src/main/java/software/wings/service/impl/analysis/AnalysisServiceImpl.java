package software.wings.service.impl.analysis;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.common.base.Preconditions;

import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
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
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.states.AbstractAnalysisState;
import software.wings.sm.states.AbstractLogAnalysisState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
public class AnalysisServiceImpl implements AnalysisService {
  private static final String CLUSTER_ML_SHELL_FILE_NAME = "run_cluster_log_ml.sh";
  private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);
  private static int NUM_OF_FIRSTL_LEVEL_CLUSTERING_THREADS =
      StringUtils.isBlank(System.getProperty("clustering.threads"))
      ? 10
      : Integer.parseInt(System.getProperty("clustering.threads"));
  private final Random random = new Random();

  public static final StateType[] logAnalysisStates = new StateType[] {StateType.SPLUNKV2, StateType.ELK};

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject protected WorkflowExecutionService workflowExecutionService;
  @Inject protected MainConfiguration configuration;

  private ExecutorService firstLevelClusteringService =
      Executors.newFixedThreadPool(NUM_OF_FIRSTL_LEVEL_CLUSTERING_THREADS, r -> new Thread(r, "clustering_thread"));

  @Override
  public Boolean saveLogData(StateType stateType, String accountId, String appId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, ClusterLevel clusterLevel,
      List<LogElement> logData) throws IOException {
    logger.info("inserting " + logData.size() + " pieces of log data");
    List<LogDataRecord> logDataRecords = LogDataRecord.generateDataRecords(
        stateType, appId, stateExecutionId, workflowId, workflowExecutionId, serviceId, clusterLevel, logData);
    wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);
    logger.info("inserted " + logDataRecords.size() + " LogDataRecord to persistence layer.");

    if (clusterLevel == ClusterLevel.L0 && !logData.isEmpty()) {
      switch (stateType) {
        case ELK:
          final LogElement log = logData.get(0);
          final LogRequest logRequest = new LogRequest(log.getQuery(), appId, stateExecutionId, workflowId, serviceId,
              Collections.singleton(log.getHost()), log.getLogCollectionMinute());
          firstLevelClusteringService.submit(new LogMessageClusterTask(
              stateType, accountId, workflowExecutionId, ClusterLevel.L0, ClusterLevel.L1, logRequest));
          break;
        default:
          throw new IllegalStateException("invalid state: " + stateType);
      }
    }
    return true;
  }

  @Override
  public void finalizeLogCollection(
      String accountId, StateType stateType, String workflowExecutionId, LogRequest logRequest) {
    new LogMessageClusterTask(stateType, accountId, workflowExecutionId, ClusterLevel.L1, ClusterLevel.L2, logRequest)
        .run();
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
  public boolean deleteProcessed(LogRequest logRequest, StateType stateType, ClusterLevel clusterLevel) {
    Query<LogDataRecord> splunkLogDataRecords = wingsPersistence.createQuery(LogDataRecord.class)
                                                    .field("stateType")
                                                    .equal(stateType)
                                                    .field("stateExecutionId")
                                                    .equal(logRequest.getStateExecutionId())
                                                    .field("applicationId")
                                                    .equal(logRequest.getApplicationId())
                                                    .field("clusterLevel")
                                                    .equal(clusterLevel)
                                                    .field("query")
                                                    .equal(logRequest.getQuery())
                                                    .field("host")
                                                    .hasAnyOf(logRequest.getNodes())
                                                    .field("logCollectionMinute")
                                                    .equal(logRequest.getLogCollectionMinute());

    return wingsPersistence.delete(splunkLogDataRecords);
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
    wingsPersistence.delete(wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                .field("applicationId")
                                .equal(mlAnalysisResponse.getApplicationId())
                                .field("stateExecutionId")
                                .equal(mlAnalysisResponse.getStateExecutionId())
                                .field("stateType")
                                .equal(stateType));

    mlAnalysisResponse.setStateType(stateType);
    wingsPersistence.save(mlAnalysisResponse);
    logger.debug(
        "inserted ml LogMLAnalysisRecord to persistence layer for app: " + mlAnalysisResponse.getApplicationId()
        + " StateExecutionInstanceId: " + mlAnalysisResponse.getStateExecutionId());
    return true;
  }

  @Override
  public LogMLAnalysisRecord getLogAnalysisRecords(
      String applicationId, String stateExecutionId, String query, StateType stateType) {
    Query<LogMLAnalysisRecord> splunkLogMLAnalysisRecords = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                                .field("stateExecutionId")
                                                                .equal(stateExecutionId)
                                                                .field("applicationId")
                                                                .equal(applicationId)
                                                                .field("query")
                                                                .equal(query)
                                                                .field("stateType")
                                                                .equal(stateType);
    return wingsPersistence.executeGetOneQuery(splunkLogMLAnalysisRecords);
  }

  @Override
  public LogMLAnalysisSummary getAnalysisSummary(String stateExecutionId, String applicationId, StateType stateType) {
    Query<LogMLAnalysisRecord> splunkLogMLAnalysisRecords = wingsPersistence.createQuery(LogMLAnalysisRecord.class)
                                                                .field("stateExecutionId")
                                                                .equal(stateExecutionId)
                                                                .field("applicationId")
                                                                .equal(applicationId)
                                                                .field("stateType")
                                                                .equal(stateType);
    LogMLAnalysisRecord analysisRecord = wingsPersistence.executeGetOneQuery(splunkLogMLAnalysisRecords);
    if (analysisRecord == null) {
      return null;
    }
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

  private class LogMessageClusterTask implements Runnable {
    private final StateType stateType;
    private final String accountId;
    private final String workflowExecutionId;
    private final ClusterLevel fromLevel;
    private final ClusterLevel toLevel;
    private final LogRequest logRequest;
    private final String serverUrl;
    private final String pythonScriptRoot;

    private LogMessageClusterTask(StateType stateType, String accountId, String workflowExecutionId,
        ClusterLevel fromLevel, ClusterLevel toLevel, LogRequest logRequest) {
      this.stateType = stateType;
      this.accountId = accountId;
      this.workflowExecutionId = workflowExecutionId;
      this.fromLevel = fromLevel;
      this.toLevel = toLevel;
      this.logRequest = logRequest;
      String protocol = AnalysisServiceImpl.this.configuration.isSslEnabled() ? "https" : "http";
      this.serverUrl = protocol + "://localhost:" + AnalysisServiceImpl.this.configuration.getApplicationPort();
      this.pythonScriptRoot = System.getenv(AbstractLogAnalysisState.LOG_ML_ROOT);
      Preconditions.checkState(!StringUtils.isBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");
    }

    @Override
    public void run() {
      try {
        final String inputLogsUrl = this.serverUrl + "/api/" + AbstractLogAnalysisState.getStateBaseUrl(stateType)
            + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + accountId
            + "&compareCurrent=true&clusterLevel=" + fromLevel.name();
        String clusteredLogSaveUrl = this.serverUrl + "/api/" + AbstractLogAnalysisState.getStateBaseUrl(stateType)
            + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL + "?accountId=" + accountId
            + "&stateExecutionId=" + logRequest.getStateExecutionId() + "&workflowId=" + logRequest.getWorkflowId()
            + "&workflowExecutionId=" + workflowExecutionId + "&serviceId=" + logRequest.getServiceId()
            + "&appId=" + logRequest.getApplicationId() + "&clusterLevel=" + toLevel.name();

        final List<String> command = new ArrayList<>();
        command.add(this.pythonScriptRoot + "/" + CLUSTER_ML_SHELL_FILE_NAME);
        command.add("--input_url");
        command.add(inputLogsUrl);
        command.add("--output_url");
        command.add(clusteredLogSaveUrl);
        command.add("--auth_token="
            + AbstractAnalysisState.generateAuthToken(configuration.getPortal().getJwtExternalServiceSecret()));
        command.add("--application_id=" + logRequest.getApplicationId());
        command.add("--workflow_id=" + logRequest.getWorkflowId());
        command.add("--state_execution_id=" + logRequest.getStateExecutionId());
        command.add("--service_id=" + logRequest.getServiceId());
        command.add("--nodes");
        command.addAll(logRequest.getNodes());
        command.add("--sim_threshold");
        command.add(String.valueOf(0.99));
        command.add("--log_collection_minute");
        command.add(String.valueOf(logRequest.getLogCollectionMinute()));
        command.add("--cluster_level");
        command.add(String.valueOf(toLevel.getLevel()));
        command.add("--query=" + logRequest.getQuery());

        final ProcessResult result =
            new ProcessExecutor(command)
                .redirectOutput(
                    Slf4jStream
                        .of(LoggerFactory.getLogger(getClass().getName() + "." + logRequest.getStateExecutionId()))
                        .asInfo())
                .execute();

        switch (result.getExitValue()) {
          case 0:
            logger.info("First level clustering done for " + logRequest);
            break;
          default:
            logger.error("First level clustering failed for " + logRequest);
        }

      } catch (Exception e) {
        logger.error("First level clustering failed for " + logRequest, e);
      }
      deleteProcessed(logRequest, stateType, fromLevel);
    }
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
    wingsPersistence.delete(deleteQuery);
  }
}
