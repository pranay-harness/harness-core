package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.VerificationConstants.LAMBDA_HOST_NAME;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_CURRENT;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.states.DynatraceState.CONTROL_HOST_NAME;
import static software.wings.sm.states.DynatraceState.TEST_HOST_NAME;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.ExceptionUtils;
import io.harness.serializer.JsonUtils;
import io.harness.time.Timestamp;
import io.harness.version.VersionInfoManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.intellij.lang.annotations.Language;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.PcfInstanceElement;
import software.wings.beans.GcpConfig;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.VerificationLogContext;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.DataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.DeploymentTimeSeriesAnalysis;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.stackdriver.StackDriverDataCollectionInfo;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 9/25/17.
 */
@Slf4j
public abstract class AbstractMetricAnalysisState extends AbstractAnalysisState {
  public static final int SMOOTH_WINDOW = 3;
  public static final int MIN_REQUESTS_PER_MINUTE = 10;
  public static final int COMPARISON_WINDOW = 1;
  public static final int PARALLEL_PROCESSES = 7;
  public static final int CANARY_DAYS_TO_COLLECT = 7;
  private static String DEMO_REQUEST_BODY;
  private static String DEMO_RESPONSE_BODY;
  private String customThresholdRefId;

  public void setCustomThresholdRefId(String crIds) {
    customThresholdRefId = crIds;
  }

  public String getCustomThresholdRefId() {
    return customThresholdRefId;
  }

  static {
    initDemoParams();
  }
  private static void initDemoParams() {
    try {
      String json = IOUtils.toString(
          AbstractAnalysisState.class.getResourceAsStream("cv-demo-api-call-logs.json"), StandardCharsets.UTF_8.name());
      JsonNode node = JsonUtils.readTree(json).get("metrics");
      DEMO_REQUEST_BODY = JsonUtils.asPrettyJson(node.get("request"));
      DEMO_RESPONSE_BODY = JsonUtils.asPrettyJson(node.get("response"));
    } catch (IOException e) {
      throw new RuntimeException("Could not read demo data from resources");
    }
  }
  @Transient @Inject protected MetricDataAnalysisService metricAnalysisService;
  @Transient @Inject protected VersionInfoManager versionInfoManager;

  public AbstractMetricAnalysisState(String name, StateType stateType) {
    super(name, stateType.name());
  }

  private void cleanUpForRetry(ExecutionContext executionContext) {
    metricAnalysisService.cleanUpForMetricRetry(executionContext.getStateExecutionInstanceId());
  }

  protected abstract String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      VerificationStateAnalysisExecutionData executionData, Map<String, String> hosts);

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try (VerificationLogContext ignored = new VerificationLogContext(context.getAccountId(), null,
             context.getStateExecutionInstanceId(), StateType.valueOf(getStateType()), OVERRIDE_ERROR)) {
      getLogger().info("Executing state {}", context.getStateExecutionInstanceId());
      Logger activityLogger = cvActivityLogService.getLoggerByStateExecutionId(
          context.getAccountId(), context.getStateExecutionInstanceId());
      if (context.isRetry()) {
        activityLogger.info(RETRYING_VERIFICATION_STATE_MSG);
      }
      String corelationId = UUID.randomUUID().toString();
      String delegateTaskId = null;
      VerificationStateAnalysisExecutionData executionData;
      try {
        cleanUpForRetry(context);
        AnalysisContext analysisContext = getAnalysisContext(context, corelationId);
        getLogger().info("context: {}", analysisContext);

        if (!checkLicense(appService.getAccountIdByAppId(context.getAppId()), StateType.valueOf(getStateType()),
                context.getStateExecutionInstanceId())) {
          return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS,
              "Your license type does not support running this verification. Skipping Analysis");
        }

        if (unresolvedHosts(analysisContext)) {
          return generateAnalysisResponse(analysisContext, ExecutionStatus.FAILED,
              "The expression " + hostnameTemplate + " could not be resolved for hosts");
        }
        if (analysisContext.isHistoricalDataCollection()) {
          Map<String, String> testNodeMap = analysisContext.getTestNodes();
          Map<String, String> controlNodeMap = analysisContext.getControlNodes();
          testNodeMap.put(TEST_HOST_NAME, DEFAULT_GROUP_NAME);
          analysisContext.setTestNodes(testNodeMap);

          controlNodeMap.put(CONTROL_HOST_NAME, DEFAULT_GROUP_NAME);
          for (int i = 1; i <= CANARY_DAYS_TO_COLLECT; ++i) {
            controlNodeMap.put(CONTROL_HOST_NAME + "-" + i, DEFAULT_GROUP_NAME);
          }
          analysisContext.setControlNodes(controlNodeMap);
        }
        saveMetaDataForDashboard(analysisContext.getAccountId(), context);

        if (isDemoPath(analysisContext)) {
          boolean failedState =
              settingsService.get(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("dev");
          generateDemoActivityLogs(activityLogger, failedState);
          generateDemoThirdPartyApiCallLogs(context.getAccountId(), context.getStateExecutionInstanceId(), failedState);
          return getDemoExecutionResponse(analysisContext);
        }

        Map<String, String> canaryNewHostNames = analysisContext.getTestNodes();
        if (isAwsLambdaState(context)) {
          canaryNewHostNames.put(LAMBDA_HOST_NAME, DEFAULT_GROUP_NAME);
        }

        if (isAwsECSState(context)) {
          CloudWatchState cloudWatchState = (CloudWatchState) this;
          if (isNotEmpty(cloudWatchState.fetchEcsMetrics())) {
            for (String clusterName : cloudWatchState.fetchEcsMetrics().keySet()) {
              canaryNewHostNames.put(clusterName, DEFAULT_GROUP_NAME);
            }
          }
        }
        if (getStateType().equals(StateType.CLOUD_WATCH.name())) {
          CloudWatchState cloudWatchState = (CloudWatchState) this;
          if (isNotEmpty(cloudWatchState.fetchLoadBalancerMetrics())) {
            for (String lbName : cloudWatchState.fetchLoadBalancerMetrics().keySet()) {
              canaryNewHostNames.put(lbName, DEFAULT_GROUP_NAME);
            }
          }
        }

        if (isEmpty(canaryNewHostNames) && !isAwsLambdaState(context)) {
          getLogger().warn(
              "id: {}, Could not find test nodes to compare the data", context.getStateExecutionInstanceId());
          return generateAnalysisResponse(analysisContext, ExecutionStatus.FAILED,
              "Could not find newly deployed instances. Please ensure that new workflow resulted in actual deployment.");
        }

        Map<String, String> lastExecutionNodes = analysisContext.getControlNodes();
        if (isEmpty(lastExecutionNodes) && !isAwsLambdaState(context)) {
          if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
            getLogger().info("No nodes with older version found to compare the logs. Skipping analysis");
            return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS,
                "As no previous version instances exist for comparison, analysis will be skipped. Check your setup if this is the first deployment or if the previous instances have been deleted or replaced.");
          }

          getLogger().info("It seems that there is no successful run for this workflow yet. "
              + "Metric data will be collected to be analyzed for next deployment run");
        }

        if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            && lastExecutionNodes.equals(canaryNewHostNames)) {
          getLogger().warn("Control and test nodes are same. Will not be running Log analysis");
          return generateAnalysisResponse(analysisContext, ExecutionStatus.FAILED,
              "Skipping analysis because both the new and old instances of the service and environment are same. Please check your setup.");
        }

        String responseMessage = "Metric Verification running";
        String baselineWorkflowExecutionId = null;
        if (getComparisonStrategy() == COMPARE_WITH_PREVIOUS) {
          WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
          baselineWorkflowExecutionId = workflowExecutionBaselineService.getBaselineExecutionId(context.getAppId(),
              context.getWorkflowId(), workflowStandardParams.getEnv().getUuid(), analysisContext.getServiceId());
          if (isEmpty(baselineWorkflowExecutionId)) {
            responseMessage = "No baseline was set for the workflow. Workflow running with auto baseline.";
            getLogger().info(responseMessage);
            baselineWorkflowExecutionId =
                metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(analysisContext.getStateType(),
                    analysisContext.getAppId(), analysisContext.getWorkflowId(), analysisContext.getServiceId(),
                    getPhaseInfraMappingId(context), workflowStandardParams.getEnv().getUuid());
          } else {
            responseMessage = "Baseline is fixed for the workflow. Analyzing against fixed baseline.";
            getLogger().info("Baseline execution is {}", baselineWorkflowExecutionId);
          }
          if (baselineWorkflowExecutionId == null) {
            responseMessage += " No previous execution found. This will be the baseline run";
            getLogger().warn("No previous execution found. This will be the baseline run");
          }
          analysisContext.setPrevWorkflowExecutionId(baselineWorkflowExecutionId);
        }

        executionData =
            VerificationStateAnalysisExecutionData.builder()
                .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                .serverConfigId(getAnalysisServerConfigId())
                .canaryNewHostNames(canaryNewHostNames.keySet())
                .lastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : lastExecutionNodes.keySet())
                .correlationId(analysisContext.getCorrelationId())
                .canaryNewHostNames(analysisContext.getTestNodes().keySet())
                .lastExecutionNodes(analysisContext.getControlNodes().keySet())
                .baselineExecutionId(baselineWorkflowExecutionId)
                .comparisonStrategy(getComparisonStrategy())
                .build();
        executionData.setErrorMsg(responseMessage);
        executionData.setStatus(ExecutionStatus.RUNNING);
        Map<String, String> hostsToCollect = new HashMap<>();
        if (getComparisonStrategy() == COMPARE_WITH_PREVIOUS) {
          hostsToCollect.putAll(canaryNewHostNames);

        } else {
          hostsToCollect.putAll(canaryNewHostNames);
          hostsToCollect.putAll(lastExecutionNodes);
        }

        getLogger().info("triggering data collection for {} state", getStateType());
        hostsToCollect.remove(null);
        createAndSaveMetricGroups(context, hostsToCollect);
        if (isCVTaskEnqueuingEnabled(context.getAccountId())) {
          getLogger().info("Data collection will be done with cv tasks.");
          analysisContext.setDataCollectionInfov2(createDataCollectionInfo(context, hostsToCollect));
        } else if (isEligibleForPerMinuteTask(context.getAccountId())) {
          getLogger().info("Per Minute data collection will be done for triggering delegate task");
        } else {
          delegateTaskId = triggerAnalysisDataCollection(context, analysisContext, executionData, hostsToCollect);
          getLogger().info(
              "triggered data collection for {} state, delegateTaskId: {}", getStateType(), delegateTaskId);
        }
        logDataCollectionTriggeredMessage(activityLogger);
        VerificationDataAnalysisResponse response =
            VerificationDataAnalysisResponse.builder().stateExecutionData(executionData).build();
        response.setExecutionStatus(ExecutionStatus.RUNNING);
        scheduleAnalysisCronJob(analysisContext, delegateTaskId);
        activityLogger.info(responseMessage);
        return ExecutionResponse.builder()
            .async(true)
            .correlationIds(Collections.singletonList(executionData.getCorrelationId()))
            .executionStatus(ExecutionStatus.RUNNING)
            .errorMessage(responseMessage)
            .stateExecutionData(executionData)
            .build();
      } catch (Exception ex) {
        // set the CV Metadata status to ERROR as well.
        activityLogger.error("Data collection failed: " + ex.getMessage());
        getLogger().error("metric analysis state {} failed", context.getStateExecutionInstanceId(), ex);
        continuousVerificationService.setMetaDataExecutionStatus(
            context.getStateExecutionInstanceId(), ExecutionStatus.ERROR, true, false);
        VerificationStateAnalysisExecutionData stateAnalysisExecutionData =
            VerificationStateAnalysisExecutionData.builder()
                .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                .serverConfigId(getAnalysisServerConfigId())
                .comparisonStrategy(getComparisonStrategy())
                .build();
        stateAnalysisExecutionData.setErrorMsg(ex.getMessage());
        stateAnalysisExecutionData.setStatus(ExecutionStatus.ERROR);
        return ExecutionResponse.builder()
            .async(false)
            .correlationIds(Collections.singletonList(corelationId))
            .executionStatus(ExecutionStatus.ERROR)
            .errorMessage(ExceptionUtils.getMessage(ex))
            .stateExecutionData(stateAnalysisExecutionData)
            .build();
      }
    }
  }

  protected DataCollectionInfoV2 createDataCollectionInfo(
      ExecutionContext context, Map<String, String> hostsToCollect) {
    throw new RuntimeException(
        "Not implemented. Designed to override."); // TODO: this method needs to be abstract eventually.
  }

  protected void createAndSaveMetricGroups(ExecutionContext context, Map<String, String> hostsToCollect) {
    Map<String, TimeSeriesMlAnalysisGroupInfo> metricGroups = new HashMap<>();
    Set<String> hostGroups = new HashSet<>(hostsToCollect.values());
    getLogger().info("for state {} saving host groups are {}", context.getStateExecutionInstanceId(), hostGroups);
    hostGroups.forEach(hostGroup
        -> metricGroups.put(hostGroup,
            TimeSeriesMlAnalysisGroupInfo.builder()
                .groupName(hostGroup)
                .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
                .build()));
    getLogger().info("for state {} saving metric groups {}", context.getStateExecutionInstanceId(), metricGroups);
    metricAnalysisService.saveMetricGroups(
        context.getAppId(), StateType.valueOf(getStateType()), context.getStateExecutionInstanceId(), metricGroups);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext executionContext, Map<String, ResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    VerificationDataAnalysisResponse executionResponse =
        (VerificationDataAnalysisResponse) response.values().iterator().next();

    if (ExecutionStatus.isBrokeStatus(executionResponse.getExecutionStatus())) {
      return getErrorExecutionResponse(executionContext, executionResponse);
    }

    AnalysisContext context =
        wingsPersistence.createQuery(AnalysisContext.class, excludeAuthority)
            .filter(AnalysisContextKeys.stateExecutionId, executionContext.getStateExecutionInstanceId())
            .get();

    ContinuousVerificationExecutionMetaData cvMetaData =
        continuousVerificationService.getCVExecutionMetaData(context.getStateExecutionId());
    Preconditions.checkNotNull(cvMetaData);

    if (cvMetaData.isManualOverride()) {
      NewRelicMetricAnalysisRecord manualActionRecord =
          metricAnalysisService.getLatestLocalAnalysisRecord(context.getStateExecutionId());
      Preconditions.checkNotNull(manualActionRecord);
      return createExecutionResponse(context, cvMetaData.getExecutionStatus(), manualActionRecord.getMessage(), false);
    }

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = metricAnalysisService.getMetricsAnalysis(
        context.getStateExecutionId(), Optional.empty(), Optional.empty(), false);
    if (deploymentTimeSeriesAnalysis == null || isEmpty(deploymentTimeSeriesAnalysis.getMetricAnalyses())) {
      getLogger().info("for {} No analysis summary.", context.getStateExecutionId());
      executionStatus = isQAVerificationPath(context.getAccountId(), context.getAppId()) ? ExecutionStatus.FAILED
                                                                                         : ExecutionStatus.SUCCESS;
      continuousVerificationService.setMetaDataExecutionStatus(
          context.getStateExecutionId(), executionStatus, true, false);
      return generateAnalysisResponse(context, executionStatus, "No Analysis result found");
    }

    getLogger().info(
        "found analysisSummary with analysis records {}", deploymentTimeSeriesAnalysis.getMetricAnalyses().size());

    // Fail state when either risk is high or tolerance is low but risk is not low
    for (NewRelicMetricAnalysis metricAnalysisRecord : deploymentTimeSeriesAnalysis.getMetricAnalyses()) {
      if (metricAnalysisRecord.getRiskLevel() == RiskLevel.HIGH
          || (AnalysisTolerance.fromInt(context.getTolerance()) == AnalysisTolerance.LOW
                 && metricAnalysisRecord.getRiskLevel() != RiskLevel.LOW)) {
        executionStatus = ExecutionStatus.FAILED;
        break;
      }
    }

    executionStatus =
        isQAVerificationPath(context.getAccountId(), context.getAppId()) ? ExecutionStatus.SUCCESS : executionStatus;
    getLogger().info("State done with status {}, id: {}", executionStatus, context.getStateExecutionId());
    executionResponse.getStateExecutionData().setStatus(executionStatus);
    metricAnalysisService.saveRawDataToGoogleDataStore(
        context.getAccountId(), context.getStateExecutionId(), executionStatus, context.getServiceId());
    continuousVerificationService.setMetaDataExecutionStatus(
        context.getStateExecutionId(), executionStatus, false, false);

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .stateExecutionData(executionResponse.getStateExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    continuousVerificationService.setMetaDataExecutionStatus(
        context.getStateExecutionInstanceId(), ExecutionStatus.ABORTED, true, false);
  }

  @Override
  protected ExecutionResponse generateAnalysisResponse(
      AnalysisContext context, ExecutionStatus status, String message) {
    NewRelicMetricAnalysisRecord metricAnalysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                            .accountId(context.getAccountId())
                                                            .message(message)
                                                            .appId(context.getAppId())
                                                            .stateType(StateType.valueOf(getStateType()))
                                                            .stateExecutionId(context.getStateExecutionId())
                                                            .workflowExecutionId(context.getWorkflowExecutionId())
                                                            .build();
    wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(metricAnalysisRecord));
    return createExecutionResponse(context, status, message, true);
  }

  private AnalysisContext getAnalysisContext(ExecutionContext context, String correlationId) {
    Map<String, String> controlNodes = new HashMap<>();
    Map<String, String> testNodes = new HashMap<>();

    if (isNewInstanceFieldPopulated(context)) {
      populateNewAndOldHostNames(context, controlNodes, testNodes);
    } else {
      controlNodes =
          getComparisonStrategy() == COMPARE_WITH_PREVIOUS ? Collections.emptyMap() : getLastExecutionNodes(context);
      testNodes = getCanaryNewHostNames(context);
    }
    if (getComparisonStrategy() == COMPARE_WITH_CURRENT) {
      getLogger().info("For {}, the lastExecutionNodes returned for canary is {}",
          context.getStateExecutionInstanceId(), controlNodes);
    }
    testNodes.keySet().forEach(controlNodes::remove);
    campareAndLogNodesUsingNewInstanceAPI(context, testNodes, controlNodes);

    int timeDurationInt = Integer.parseInt(getTimeDuration());
    String accountId = appService.get(context.getAppId()).getAccountId();
    boolean isHistoricalDataCollection = isHistoricalAnalysis(context.getAccountId());
    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(appService.get(context.getAppId()).getAccountId())
            .appId(context.getAppId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .serviceId(getPhaseServiceId(context))
            .analysisType(MLAnalysisType.TIME_SERIES)
            .controlNodes(controlNodes)
            .testNodes(testNodes)
            .isSSL(configuration.isSslEnabled())
            .appPort(configuration.getApplicationPort())
            .comparisonStrategy(getComparisonStrategy())
            .timeDuration(timeDurationInt)
            .stateType(StateType.valueOf(getStateType()))
            .analysisServerConfigId(getAnalysisServerConfigId())
            .correlationId(correlationId)
            .smooth_window(SMOOTH_WINDOW)
            .tolerance(getAnalysisTolerance().tolerance())
            .minimumRequestsPerMinute(MIN_REQUESTS_PER_MINUTE)
            .comparisonWindow(COMPARISON_WINDOW)
            .customThresholdRefId(customThresholdRefId)
            .startDataCollectionMinute(TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()))
            .parallelProcesses(PARALLEL_PROCESSES)
            .managerVersion(versionInfoManager.getVersionInfo().getVersion())
            .isHistoricalDataCollection(isHistoricalDataCollection)
            .initialDelaySeconds(getDelaySeconds(initialAnalysisDelay))
            .dataCollectionIntervalMins(getDataCollectionRate())
            .build();
    if (getCVTaskFeatureName().isPresent()) {
      analysisContext.setFeatureFlag(
          getCVTaskFeatureName().get(), isCVTaskEnqueuingEnabled(analysisContext.getAccountId()));
    }
    if (isEligibleForPerMinuteTask(accountId)) {
      DataCollectionInfo dataCollectionInfo = createDataCollectionInfo(context);
      analysisContext.setDataCollectionInfo(dataCollectionInfo);
    }
    return analysisContext;
  }

  public int getDataCollectionRate() {
    return 1;
  }

  private DataCollectionInfo createDataCollectionInfo(ExecutionContext context) {
    StateType stateType = StateType.valueOf(getStateType());
    switch (stateType) {
      case STACK_DRIVER:
        TimeSeriesMlAnalysisType analyzedTierAnalysisType =
            getComparisonStrategy() == AnalysisComparisonStrategy.PREDICTIVE ? TimeSeriesMlAnalysisType.PREDICTIVE
                                                                             : TimeSeriesMlAnalysisType.COMPARATIVE;
        StackDriverState stackDriverState = (StackDriverState) this;
        GcpConfig gcpConfig = (GcpConfig) settingsService.get(getAnalysisServerConfigId()).getValue();
        ((StackDriverState) this).saveMetricTemplates(context);
        return StackDriverDataCollectionInfo.builder()
            .gcpConfig(gcpConfig)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(context.getWorkflowId())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .timeSeriesMlAnalysisType(analyzedTierAnalysisType)
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .encryptedDataDetails(
                secretManager.getEncryptionDetails(gcpConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .hosts(new HashMap<>())
            .timeSeriesToCollect(stackDriverState.fetchMetricDefinitions())
            .build();

      default:
        return null;
    }
  }

  @Override
  protected String getPcfHostName(PcfInstanceElement pcfInstanceElement, boolean includePrevious) {
    if ((includePrevious && !pcfInstanceElement.isUpsize()) || (!includePrevious && pcfInstanceElement.isUpsize())) {
      return pcfInstanceElement.getDisplayName() + ":" + pcfInstanceElement.getInstanceIndex();
    }

    return null;
  }

  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  public void setAnalysisTolerance(String tolerance) {
    this.tolerance = tolerance;
  }

  private void generateDemoThirdPartyApiCallLogs(String accountId, String stateExecutionId, boolean failedState) {
    super.generateDemoThirdPartyApiCallLogs(
        accountId, stateExecutionId, failedState, demoRequestBody(), demoMetricResponse());
  }
  @Language("JSON")
  private String demoRequestBody() {
    return DEMO_REQUEST_BODY;
  }
  @Language("JSON")
  private String demoMetricResponse() {
    return DEMO_RESPONSE_BODY;
  }
}
