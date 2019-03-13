package software.wings.service.impl.analysis;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeCount;
import static software.wings.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.common.VerificationConstants.DEMO_APPLICAITON_ID;
import static software.wings.common.VerificationConstants.DEMO_FAILURE_LOG_STATE_EXECUTION_ID;
import static software.wings.common.VerificationConstants.DEMO_SUCCESS_LOG_STATE_EXECUTION_ID;
import static software.wings.common.VerificationConstants.GLOBAL_APP_ID;
import static software.wings.common.VerificationConstants.IGNORED_ERRORS_METRIC_NAME;
import static software.wings.delegatetasks.ElkLogzDataCollectionTask.parseElkResponse;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageResponse;
import io.harness.beans.SortOrder.OrderType;
import io.harness.eraro.ErrorCode;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.serializer.JsonUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.ElkConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.config.LogzConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.resources.AccountResource;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.splunk.LogMLClusterScores;
import software.wings.service.impl.splunk.LogMLClusterScores.LogMLScore;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
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
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.verification.log.LogsCVConfiguration;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
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

  private static final StateType[] logAnalysisStates = new StateType[] {StateType.SPLUNKV2, StateType.ELK};

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject protected SettingsService settingsService;
  @Inject protected WorkflowExecutionService workflowExecutionService;
  @Inject protected DelegateServiceImpl delegateService;
  @Inject protected SecretManager secretManager;
  @Inject private AccountResource accountResource;
  @Inject private HarnessMetricRegistry metricRegistry;
  @Inject private UsageMetricsHelper usageMetricsHelper;

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

    // delete verification service tasks
    wingsPersistence.delete(
        wingsPersistence.createQuery(AnalysisContext.class).filter("stateExecutionId", stateExecutionId));
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

  @Override
  public LogMLAnalysisSummary getAnalysisSummaryForDemo(
      String stateExecutionId, String applicationId, StateType stateType) {
    logger.info("Creating log analysis summary for demo {}", stateExecutionId);
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(applicationId, stateExecutionId);
    if (stateExecutionInstance == null) {
      logger.error("State execution instance not found for {}", stateExecutionId);
      return null;
    }

    SettingAttribute settingAttribute = settingsService.get(
        ((LogAnalysisExecutionData) stateExecutionInstance.getStateExecutionData()).getServerConfigId());

    if (settingAttribute.getName().toLowerCase().endsWith("dev")
        || settingAttribute.getName().toLowerCase().endsWith("prod")) {
      if (stateExecutionInstance.getStatus() == ExecutionStatus.SUCCESS) {
        return getAnalysisSummary(
            DEMO_SUCCESS_LOG_STATE_EXECUTION_ID + stateType.getName(), DEMO_APPLICAITON_ID, stateType);
      } else {
        return getAnalysisSummary(
            DEMO_FAILURE_LOG_STATE_EXECUTION_ID + stateType.getName(), DEMO_APPLICAITON_ID, stateType);
      }
    }
    return getAnalysisSummary(stateExecutionId, applicationId, stateType);
  }

  @Override
  public List<LogMLFeedbackRecord> getMLFeedback(
      String appId, String serviceId, String workflowId, String workflowExecutionId) {
    Query<LogMLFeedbackRecord> query = wingsPersistence.createQuery(LogMLFeedbackRecord.class).filter("appId", appId);
    query.or(query.criteria("serviceId").equal(serviceId), query.criteria("workflowId").equal(workflowId),
        query.criteria("workflowExecutionId").equal(workflowExecutionId));

    return query.asList();
  }

  @Override
  public List<LogMLFeedbackRecord> getMLFeedback(String accountId, String workflowId) {
    List<LogMLFeedbackRecord> feedbackRecords = null;
    if (accountResource.isFeatureEnabled(FeatureName.GLOBAL_CV_DASH.name(), accountId).getResource()) {
      Query<LogMLFeedbackRecord> query =
          wingsPersistence.createQuery(LogMLFeedbackRecord.class).filter("workflowId", workflowId);
      feedbackRecords = query.asList();
    }
    return feedbackRecords;
  }

  @Override
  public boolean saveFeedback(LogMLFeedback feedback, StateType stateType) {
    if (!isEmpty(feedback.getLogMLFeedbackId())) {
      deleteFeedbackHelper(feedback.getLogMLFeedbackId());
    }

    StateExecutionInstance stateExecutionInstance = wingsPersistence.getWithAppId(
        StateExecutionInstance.class, feedback.getAppId(), feedback.getStateExecutionId());

    if (stateExecutionInstance == null) {
      throw new WingsException("Unable to find state execution for id " + feedback.getStateExecutionId());
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
    metricRegistry.recordGaugeInc(IGNORED_ERRORS_METRIC_NAME,
        new String[] {feedback.getLogMLFeedbackType().toString(), stateType.toString(), feedback.getAppId(),
            stateExecutionInstance.getWorkflowId()});

    return true;
  }

  @Override
  public String getLastSuccessfulWorkflowExecutionIdWithLogs(
      String stateExecutionId, StateType stateType, String appId, String serviceId, String workflowId, String query) {
    // TODO should we limit the number of executions to search in ??
    List<String> successfulExecutions = new ArrayList<>();
    List<ContinuousVerificationExecutionMetaData> cvList =
        wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class, excludeCount)
            .filter("appId", appId)
            .filter("stateType", stateType)
            .filter("workflowId", workflowId)
            .filter("executionStatus", ExecutionStatus.SUCCESS)
            .order("-workflowStartTs")
            .asList();
    logger.info("Fetched {} CVExecutionMetadata for stateExecutionId {}", cvList.size(), stateExecutionId);
    cvList.forEach(cvMetadata -> successfulExecutions.add(cvMetadata.getWorkflowExecutionId()));
    for (String successfulExecution : successfulExecutions) {
      if (wingsPersistence.createQuery(LogDataRecord.class)
              .filter("appId", appId)
              .filter("workflowExecutionId", successfulExecution)
              .filter("clusterLevel", ClusterLevel.L2)
              .filter("query", query)
              .count(new CountOptions().limit(1))
          > 0) {
        logger.info("Found an execution for auto baseline. WorkflowExecutionId {}, stateExecutionId {}",
            successfulExecution, stateExecutionId);
        return successfulExecution;
      }
    }
    if (isNotEmpty(successfulExecutions)) {
      logger.info(
          "We did not find any execution with data. Returning workflowExecution {} as baseline for stateExecutionId {}",
          cvList.get(0).getWorkflowExecutionId(), stateExecutionId);
      return cvList.get(0).getWorkflowExecutionId();
    }
    logger.warn("Could not get a successful workflow to find control nodes for stateExecutionId: {}", stateExecutionId);
    return null;
  }

  private void decompressLogAnalysisRecord(LogMLAnalysisRecord logMLAnalysisRecord) {
    if (isNotEmpty(logMLAnalysisRecord.getAnalysisDetailsCompressedJson())) {
      try {
        String decompressedAnalysisDetailsJson =
            deCompressString(logMLAnalysisRecord.getAnalysisDetailsCompressedJson());
        LogMLAnalysisRecord logAnalysisDetails =
            JsonUtils.asObject(decompressedAnalysisDetailsJson, LogMLAnalysisRecord.class);
        logMLAnalysisRecord.setUnknown_events(logAnalysisDetails.getUnknown_events());
        logMLAnalysisRecord.setTest_events(logAnalysisDetails.getTest_events());
        logMLAnalysisRecord.setControl_events(logAnalysisDetails.getControl_events());
        logMLAnalysisRecord.setControl_clusters(logAnalysisDetails.getControl_clusters());
        logMLAnalysisRecord.setUnknown_clusters(logAnalysisDetails.getUnknown_clusters());
        logMLAnalysisRecord.setTest_clusters(logAnalysisDetails.getTest_clusters());
        logMLAnalysisRecord.setIgnore_clusters(logAnalysisDetails.getIgnore_clusters());
      } catch (IOException e) {
        throw new WingsException(e);
      }
    }
  }

  private Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> getMLUserFeedbacks(
      String stateExecutionId, String appId) {
    Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> userFeedbackMap = new HashMap<>();
    userFeedbackMap.put(CLUSTER_TYPE.CONTROL, new HashMap<>());
    userFeedbackMap.put(CLUSTER_TYPE.TEST, new HashMap<>());
    userFeedbackMap.put(CLUSTER_TYPE.UNKNOWN, new HashMap<>());

    List<LogMLFeedbackRecord> logMLFeedbackRecords =
        wingsPersistence.createQuery(LogMLFeedbackRecord.class, excludeCount)
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

    decompressLogAnalysisRecord(analysisRecord);
    Map<CLUSTER_TYPE, Map<Integer, LogMLFeedbackRecord>> mlUserFeedbacks = getMLUserFeedbacks(stateExecutionId, appId);

    LogMLClusterScores logMLClusterScores =
        analysisRecord.getCluster_scores() != null ? analysisRecord.getCluster_scores() : new LogMLClusterScores();

    final LogMLAnalysisSummary analysisSummary =
        LogMLAnalysisSummary.builder()
            .query(analysisRecord.getQuery())
            .stateType(stateType)
            .analysisMinute(analysisRecord.getLogCollectionMinute())
            .score(analysisRecord.getScore() * 100)
            .baseLineExecutionId(analysisRecord.getBaseLineExecutionId())
            .controlClusters(
                computeCluster(analysisRecord.getControl_clusters(), Collections.emptyMap(), CLUSTER_TYPE.CONTROL))
            .testClusters(
                computeCluster(analysisRecord.getTest_clusters(), logMLClusterScores.getTest(), CLUSTER_TYPE.TEST))
            .unknownClusters(computeCluster(
                analysisRecord.getUnknown_clusters(), logMLClusterScores.getUnknown(), CLUSTER_TYPE.UNKNOWN))
            .ignoreClusters(
                computeCluster(analysisRecord.getIgnore_clusters(), Collections.emptyMap(), CLUSTER_TYPE.IGNORE))
            .build();

    final AnalysisContext analysisContext = wingsPersistence.createQuery(AnalysisContext.class)
                                                .filter("appId", appId)
                                                .filter("stateExecutionId", stateExecutionId)
                                                .get();

    if (analysisContext != null) {
      analysisSummary.setAnalysisComparisonStrategy(analysisContext.getComparisonStrategy());
      analysisSummary.setTimeDuration(analysisContext.getTimeDuration());
      analysisSummary.setNewVersionNodes(
          isEmpty(analysisContext.getTestNodes()) ? Collections.emptySet() : analysisContext.getTestNodes().keySet());
      analysisSummary.setPreviousVersionNodes(isEmpty(analysisContext.getControlNodes())
              ? Collections.emptySet()
              : analysisContext.getControlNodes().keySet());
    }
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
    populateWorkflowDetails(analysisSummary, analysisContext);
    return analysisSummary;
  }

  private void populateWorkflowDetails(LogMLAnalysisSummary analysisSummary, AnalysisContext analysisContext) {
    if (analysisSummary == null || analysisContext == null) {
      return;
    }

    long minAnalyzedMinute = getLogRecordMinute(
        analysisContext.getAppId(), analysisContext.getStateExecutionId(), ClusterLevel.HF, OrderType.ASC);

    if (minAnalyzedMinute < 0) {
      // no analysis yet
      return;
    }

    long maxAnalyzedMinute = getLogRecordMinute(
        analysisContext.getAppId(), analysisContext.getStateExecutionId(), ClusterLevel.HF, OrderType.DESC);
    if (AnalysisComparisonStrategy.PREDICTIVE.equals(analysisContext.getComparisonStrategy())) {
      if (isNotEmpty(analysisContext.getPredictiveCvConfigId())) {
        LogsCVConfiguration logsCVConfiguration =
            wingsPersistence.get(LogsCVConfiguration.class, analysisContext.getPredictiveCvConfigId());
        analysisSummary.setBaselineStartTime(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute()));
        analysisSummary.setBaselineEndTime(TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineEndMinute()));
        if (maxAnalyzedMinute > 0) {
          if (maxAnalyzedMinute > logsCVConfiguration.getBaselineEndMinute()) {
            maxAnalyzedMinute -=
                logsCVConfiguration.getBaselineEndMinute() - logsCVConfiguration.getBaselineStartMinute();
          } else {
            maxAnalyzedMinute = minAnalyzedMinute;
          }
        }
      }
    } else {
      // since both min and max are inclusive
      maxAnalyzedMinute += 1;
    }

    int duration = analysisContext.getTimeDuration();
    int progressedMinutes = (int) (maxAnalyzedMinute - minAnalyzedMinute);
    analysisSummary.setProgress(Math.min(100, progressedMinutes * 100 / duration));
  }

  private long getLogRecordMinute(
      String appId, String stateExecutionId, ClusterLevel clusterLevel, OrderType orderType) {
    LogDataRecord logDataRecord =
        wingsPersistence.createQuery(LogDataRecord.class)
            .filter("appId", appId)
            .filter("stateExecutionId", stateExecutionId)
            .filter("clusterLevel", clusterLevel)
            .order(orderType == OrderType.DESC ? "-logCollectionMinute" : "logCollectionMinute")
            .get();

    return logDataRecord == null ? -1 : logDataRecord.getLogCollectionMinute();
  }

  @Override
  public void validateConfig(
      final SettingAttribute settingAttribute, StateType stateType, List<EncryptedDataDetail> encryptedDataDetails) {
    ErrorCode errorCode = null;

    try {
      switch (stateType) {
        case SPLUNKV2:
          errorCode = ErrorCode.SPLUNK_CONFIGURATION_ERROR;
          SyncTaskContext splunkTaskContext = SyncTaskContext.builder()
                                                  .accountId(settingAttribute.getAccountId())
                                                  .appId(GLOBAL_APP_ID)
                                                  .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                                  .build();
          delegateProxyFactory.get(SplunkDelegateService.class, splunkTaskContext)
              .validateConfig((SplunkConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        case ELK:
          errorCode = ErrorCode.ELK_CONFIGURATION_ERROR;
          SyncTaskContext elkTaskContext = SyncTaskContext.builder()
                                               .accountId(settingAttribute.getAccountId())
                                               .appId(GLOBAL_APP_ID)
                                               .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 2)
                                               .build();
          delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .validateConfig((ElkConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        case LOGZ:
          errorCode = ErrorCode.LOGZ_CONFIGURATION_ERROR;
          SyncTaskContext logzTaskContext = SyncTaskContext.builder()
                                                .accountId(settingAttribute.getAccountId())
                                                .appId(GLOBAL_APP_ID)
                                                .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                                .build();
          delegateProxyFactory.get(LogzDelegateService.class, logzTaskContext)
              .validateConfig((LogzConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        case SUMO:
          errorCode = ErrorCode.SUMO_CONFIGURATION_ERROR;
          SyncTaskContext sumoTaskContext = SyncTaskContext.builder()
                                                .accountId(settingAttribute.getAccountId())
                                                .appId(GLOBAL_APP_ID)
                                                .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                                .build();
          delegateProxyFactory.get(SumoDelegateService.class, sumoTaskContext)
              .validateConfig((SumoConfig) settingAttribute.getValue(), encryptedDataDetails);
          break;
        default:
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          throw new IllegalStateException("Invalid state type: " + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode, USER, e).addParam("reason", ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public Object getLogSample(
      String accountId, String analysisServerConfigId, String index, StateType stateType, int duration) {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No " + stateType + " setting with id: " + analysisServerConfigId + " found");
    }
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    ErrorCode errorCode = null;
    try {
      switch (stateType) {
        case ELK:
          errorCode = ErrorCode.ELK_CONFIGURATION_ERROR;
          SyncTaskContext elkTaskContext = SyncTaskContext.builder()
                                               .accountId(accountId)
                                               .appId(GLOBAL_APP_ID)
                                               .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                               .build();
          return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
              .getLogSample((ElkConfig) settingAttribute.getValue(), index, true, encryptedDataDetails);
        case LOGZ:
          errorCode = ErrorCode.LOGZ_CONFIGURATION_ERROR;
          SyncTaskContext logzTaskContext = SyncTaskContext.builder()
                                                .accountId(settingAttribute.getAccountId())
                                                .appId(GLOBAL_APP_ID)
                                                .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                                .build();
          return delegateProxyFactory.get(LogzDelegateService.class, logzTaskContext)
              .getLogSample((LogzConfig) settingAttribute.getValue(), encryptedDataDetails);
        case SUMO:
          errorCode = ErrorCode.SUMO_CONFIGURATION_ERROR;
          SyncTaskContext sumoTaskContext = SyncTaskContext.builder()
                                                .accountId(accountId)
                                                .appId(GLOBAL_APP_ID)
                                                .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                                .build();
          return delegateProxyFactory.get(SumoDelegateService.class, sumoTaskContext)
              .getLogSample((SumoConfig) settingAttribute.getValue(), index, encryptedDataDetails, duration);
        default:
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          throw new IllegalStateException("Invalid state type: " + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode, e).addParam("reason", ExceptionUtils.getMessage(e));
    }
  }

  public Object getHostLogRecords(String accountId, String analysisServerConfigId, String index, ElkQueryType queryType,
      String query, String timeStampField, String timeStampFieldFormat, String messageField, String hostNameField,
      String hostName, StateType stateType, boolean formattedQuery) {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No " + stateType + " setting with id: " + analysisServerConfigId + " found");
    }
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    ErrorCode errorCode = null;
    final ElkLogFetchRequest elkFetchRequest =
        ElkLogFetchRequest.builder()
            .query(query)
            .formattedQuery(formattedQuery)
            .indices(index)
            .hostnameField(hostNameField)
            .messageField(messageField)
            .timestampField(timeStampField)
            .hosts(Sets.newHashSet(hostName))
            .startTime(TimeUnit.SECONDS.toMillis(OffsetDateTime.now().minusMinutes(15).toEpochSecond()))
            .endTime(TimeUnit.SECONDS.toMillis(OffsetDateTime.now().toEpochSecond()))
            .queryType(queryType)
            .build();
    Object searchResponse;
    try {
      switch (stateType) {
        case ELK:
          errorCode = ErrorCode.ELK_CONFIGURATION_ERROR;
          SyncTaskContext elkTaskContext = SyncTaskContext.builder()
                                               .accountId(accountId)
                                               .appId(GLOBAL_APP_ID)
                                               .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                               .build();
          searchResponse =
              delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
                  .search((ElkConfig) settingAttribute.getValue(), encryptedDataDetails, elkFetchRequest,
                      createApiCallLog(accountId, GLOBAL_APP_ID, null), ElkDelegateServiceImpl.MAX_RECORDS);
          break;
        case LOGZ:
          errorCode = ErrorCode.LOGZ_CONFIGURATION_ERROR;
          SyncTaskContext logzTaskContext = SyncTaskContext.builder()
                                                .accountId(settingAttribute.getAccountId())
                                                .appId(GLOBAL_APP_ID)
                                                .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                                .build();
          searchResponse = delegateProxyFactory.get(LogzDelegateService.class, logzTaskContext)
                               .search((LogzConfig) settingAttribute.getValue(), encryptedDataDetails, elkFetchRequest,
                                   createApiCallLog(accountId, GLOBAL_APP_ID, null));
          break;
        default:
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          throw new IllegalStateException("Invalid state type: " + stateType);
      }
    } catch (Exception e) {
      throw new WingsException(errorCode, e).addParam("reason", ExceptionUtils.getMessage(e));
    }

    try {
      parseElkResponse(searchResponse, query, timeStampField, timeStampFieldFormat, hostNameField, hostName,
          messageField, 0, false, -1, -1);
    } catch (Exception e) {
      throw new WingsException("Data fetch successful but date parsing failed.", e);
    }
    return searchResponse;
  }

  public List<LogMLClusterSummary> computeCluster(Map<String, Map<String, SplunkAnalysisCluster>> cluster,
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

  private void deleteNotRequiredLogs(StateType stateType, WorkflowExecution workflowExecution) {
    wingsPersistence.createQuery(LogDataRecord.class)
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
    wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(analysisRecord));
  }

  @Override
  public boolean isStateValid(String appId, String stateExecutionID) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appId, stateExecutionID);
    return stateExecutionInstance != null && !ExecutionStatus.isFinalStatus(stateExecutionInstance.getStatus());
  }

  @Override
  public Map<String, InstanceElement> getLastExecutionNodes(String appId, String workflowId) {
    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .filter("appId", appId)
                                              .filter("workflowId", workflowId)
                                              .filter("status", SUCCESS)
                                              .order(Sort.descending(WorkflowExecution.CREATED_AT_KEY))
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
    LogMLClusterScores logMLClusterScores =
        analysisRecord.getCluster_scores() != null ? analysisRecord.getCluster_scores() : new LogMLClusterScores();
    final LogMLAnalysisSummary analysisSummary =
        LogMLAnalysisSummary.builder()
            .query(analysisRecord.getQuery())
            .score(analysisRecord.getScore() * 100)
            .controlClusters(computeCluster(
                analysisRecord.getControl_clusters(), Collections.emptyMap(), AnalysisServiceImpl.CLUSTER_TYPE.CONTROL))
            .testClusters(computeCluster(
                analysisRecord.getTest_clusters(), logMLClusterScores.getTest(), AnalysisServiceImpl.CLUSTER_TYPE.TEST))
            .unknownClusters(computeCluster(analysisRecord.getUnknown_clusters(), logMLClusterScores.getUnknown(),
                AnalysisServiceImpl.CLUSTER_TYPE.UNKNOWN))
            .ignoreClusters(
                computeCluster(analysisRecord.getIgnore_clusters(), Collections.emptyMap(), CLUSTER_TYPE.IGNORE))
            .build();

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
