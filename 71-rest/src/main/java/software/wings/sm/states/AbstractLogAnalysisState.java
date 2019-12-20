package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.common.VerificationConstants.GA_PER_MINUTE_CV_STATES;
import static software.wings.common.VerificationConstants.PER_MINUTE_CV_STATES;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_CURRENT;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.PREDICTIVE;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.ExceptionUtils;
import io.harness.serializer.JsonUtils;
import io.harness.time.Timestamp;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.DatadogConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpConfig;
import software.wings.beans.SumoConfig;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.stackdriver.StackDriverLogDataCollectionInfo;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;
import software.wings.verification.log.LogsCVConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 7/6/17.
 */
public abstract class AbstractLogAnalysisState extends AbstractAnalysisState {
  public static final int HOST_BATCH_SIZE = 5;
  private static String DEMO_REQUEST_BODY;
  private static String DEMO_RESPONSE_BODY;
  static {
    initDemoParams();
  }
  private static void initDemoParams() {
    try {
      String json = IOUtils.toString(
          AbstractAnalysisState.class.getResourceAsStream("cv-demo-api-call-logs.json"), StandardCharsets.UTF_8.name());
      JsonNode node = JsonUtils.readTree(json).get("logs");
      DEMO_REQUEST_BODY = JsonUtils.asPrettyJson(node.get("request"));
      DEMO_RESPONSE_BODY = JsonUtils.asPrettyJson(node.get("response"));
    } catch (IOException e) {
      throw new RuntimeException("Could not read demo data from resources");
    }
  }

  protected String query;

  @Transient @Inject @SchemaIgnore protected AnalysisService analysisService;
  @Transient @Inject @SchemaIgnore protected VersionInfoManager versionInfoManager;

  @SchemaIgnore @Transient private String renderedQuery;

  public AbstractLogAnalysisState(String name, String stateType) {
    super(name, stateType);
  }

  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue("*exception*")
  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query.trim();
  }

  public String getRenderedQuery() {
    return renderedQuery;
  }

  private void cleanUpForRetry(ExecutionContext executionContext) {
    analysisService.cleanUpForLogRetry(executionContext.getStateExecutionInstanceId());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext executionContext) {
    String correlationId = UUID.randomUUID().toString();
    Logger activityLogger =
        cvActivityLogService.getLoggerByStateExecutionId(executionContext.getStateExecutionInstanceId());
    String delegateTaskId = null;
    try {
      renderedQuery = executionContext.renderExpression(query);
      getLogger().info("Executing {}", getStateType());
      cleanUpForRetry(executionContext);
      AnalysisContext analysisContext = getLogAnalysisContext(executionContext, correlationId);
      getLogger().info("context: {}", analysisContext);

      if (!checkLicense(appService.getAccountIdByAppId(executionContext.getAppId()), StateType.valueOf(getStateType()),
              executionContext.getStateExecutionInstanceId())) {
        return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS,
            "Your license type does not support running this verification. Skipping Analysis");
      }

      saveMetaDataForDashboard(analysisContext.getAccountId(), executionContext);

      Set<String> canaryNewHostNames = analysisContext.getTestNodes().keySet();
      if (isDemoPath(analysisContext)) {
        boolean failedState = settingsService.get(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("dev");
        generateDemoActivityLogs(activityLogger, failedState);
        generateDemoThirdPartyApiCallLogs(
            analysisContext.getAccountId(), analysisContext.getStateExecutionId(), failedState);
        return getDemoExecutionResponse(analysisContext);
      }

      if (isEmpty(canaryNewHostNames)) {
        getLogger().warn("Could not find test nodes to compare the data");
        return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS, "Could not find hosts to analyze!");
      }

      Set<String> lastExecutionNodes = analysisContext.getControlNodes().keySet();
      if (isEmpty(lastExecutionNodes)) {
        if (getComparisonStrategy() == COMPARE_WITH_CURRENT) {
          getLogger().info("No nodes with older version found to compare the logs. Skipping analysis");
          return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS,
              "Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.");
        }

        getLogger().warn(
            "It seems that there is no successful run for this workflow yet. Log data will be collected to be analyzed for next deployment run");
      }

      String responseMessage = "Log Verification running.";
      String baselineWorkflowExecutionId = null;
      if (getComparisonStrategy() == COMPARE_WITH_PREVIOUS) {
        WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
        baselineWorkflowExecutionId = workflowExecutionBaselineService.getBaselineExecutionId(
            analysisContext.getAppId(), analysisContext.getWorkflowId(), workflowStandardParams.getEnv().getUuid(),
            analysisContext.getServiceId());
        if (isEmpty(baselineWorkflowExecutionId)) {
          responseMessage = "No baseline was set for the workflow. Workflow running with auto baseline.";
          getLogger().info("{}", responseMessage);
          baselineWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
              analysisContext.getStateExecutionId(), analysisContext.getStateType(), analysisContext.getAppId(),
              analysisContext.getServiceId(), analysisContext.getWorkflowId(), analysisContext.getQuery(),
              getPhaseInfraMappingId(executionContext), workflowStandardParams.getEnvId());
        } else {
          responseMessage = "Baseline is pinned for the workflow. Analyzing against pinned baseline.";
          getLogger().info("Baseline is pinned for stateExecution: {}, baselineId: {}",
              analysisContext.getStateExecutionId(), baselineWorkflowExecutionId);
        }
        if (baselineWorkflowExecutionId == null) {
          responseMessage += " No previous execution found. This will be the baseline run.";
          getLogger().warn("No previous execution found. This will be the baseline run");
        }
        getLogger().info(
            "Baseline execution for {} is {}", analysisContext.getStateExecutionId(), baselineWorkflowExecutionId);
        analysisContext.setPrevWorkflowExecutionId(baselineWorkflowExecutionId);
      }
      activityLogger.info(responseMessage);

      final VerificationStateAnalysisExecutionData executionData =
          VerificationStateAnalysisExecutionData.builder()
              .stateExecutionInstanceId(analysisContext.getStateExecutionId())
              .baselineExecutionId(baselineWorkflowExecutionId)
              .serverConfigId(getAnalysisServerConfigId())
              .query(getRenderedQuery())
              .canaryNewHostNames(canaryNewHostNames)
              .lastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : new HashSet<>(lastExecutionNodes))
              .correlationId(analysisContext.getCorrelationId())
              .comparisonStrategy(getComparisonStrategy())
              .build();

      executionData.setStatus(ExecutionStatus.RUNNING);
      executionData.setErrorMsg(responseMessage);

      Set<String> hostsToBeCollected = new HashSet<>();
      if (getComparisonStrategy() == COMPARE_WITH_CURRENT && lastExecutionNodes != null) {
        hostsToBeCollected.addAll(lastExecutionNodes);
      }

      hostsToBeCollected.addAll(canaryNewHostNames);
      hostsToBeCollected.remove(null);
      getLogger().info("triggering data collection for {} state", getStateType());

      if (getCVTaskFeatureName().isPresent() && analysisContext.isFeatureFlagEnabled(getCVTaskFeatureName().get())) {
        getLogger().info("Data collection will be done with cv tasks.");
        analysisContext.setDataCollectionInfov2(createDataCollectionInfo(executionContext, hostsToBeCollected));
      } else if (isEligibleForPerMinuteTask(executionContext.getAccountId())) {
        // In case of predictive the data collection will be handled as per 24x7 logic
        // Or in case when feature flag CV_DATA_COLLECTION_JOB is enabled. Delegate task creation will be every minute
        getLogger().info("Per Minute data collection will be done for triggering delegate task");
      } else {
        delegateTaskId = triggerAnalysisDataCollection(executionContext, executionData, hostsToBeCollected);
        getLogger().info("triggered data collection for {} state, delgateTaskId: {}", getStateType(), delegateTaskId);
      }
      logDataCollectionTriggeredMessage(activityLogger);
      // Set the rendered query into the analysis context which will be used during task analysis.
      analysisContext.setQuery(getRenderedQuery());

      scheduleAnalysisCronJob(analysisContext, delegateTaskId);

      return ExecutionResponse.builder()
          .async(true)
          .correlationIds(Collections.singletonList(analysisContext.getCorrelationId()))
          .executionStatus(ExecutionStatus.RUNNING)
          .errorMessage(responseMessage)
          .stateExecutionData(executionData)
          .delegateTaskId(delegateTaskId)
          .build();
    } catch (Exception ex) {
      getLogger().error("log analysis state failed ", ex);
      // set the CV Metadata status to ERROR as well.
      activityLogger.error("Data collection failed: " + ex.getMessage());
      continuousVerificationService.setMetaDataExecutionStatus(
          executionContext.getStateExecutionInstanceId(), ExecutionStatus.ERROR, true, false);

      return ExecutionResponse.builder()
          .async(false)
          .correlationIds(Collections.singletonList(correlationId))
          .executionStatus(ExecutionStatus.ERROR)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .stateExecutionData(VerificationStateAnalysisExecutionData.builder()
                                  .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                  .serverConfigId(getAnalysisServerConfigId())
                                  .query(getRenderedQuery())
                                  .comparisonStrategy(getComparisonStrategy())
                                  .build())
          .build();
    }
  }

  protected DataCollectionInfoV2 createDataCollectionInfo(ExecutionContext context, Set<String> hosts) {
    throw new RuntimeException("Not implemented."); // TODO: this method needs to be abstract eventually.
  }

  protected abstract String triggerAnalysisDataCollection(
      ExecutionContext context, VerificationStateAnalysisExecutionData executionData, Set<String> hosts);

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext executionContext, Map<String, ResponseData> response) {
    VerificationDataAnalysisResponse executionResponse =
        (VerificationDataAnalysisResponse) response.values().iterator().next();

    if (ExecutionStatus.isBrokeStatus(executionResponse.getExecutionStatus())) {
      return getErrorExecutionResponse(executionContext, executionResponse);
    } else {
      AnalysisContext context =
          wingsPersistence.createQuery(AnalysisContext.class, excludeAuthority)
              .filter(AnalysisContextKeys.stateExecutionId, executionContext.getStateExecutionInstanceId())
              .get();
      int analysisMinute = executionResponse.getStateExecutionData().getAnalysisMinute();
      for (int i = 0; i < NUM_OF_RETRIES; i++) {
        final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
            context.getStateExecutionId(), context.getAppId(), StateType.valueOf(getStateType()));

        if (analysisSummary == null) {
          getLogger().info("No analysis summary. This can happen if there is no data with the given queries");
          continuousVerificationService.setMetaDataExecutionStatus(
              executionContext.getStateExecutionInstanceId(), ExecutionStatus.SUCCESS, true, false);
          return isQAVerificationPath(context.getAccountId(), context.getAppId())
              ? generateAnalysisResponse(context, ExecutionStatus.FAILED, "No Analysis result found")
              : generateAnalysisResponse(
                    context, ExecutionStatus.SUCCESS, "No data found with given queries. Skipped Analysis");
        }

        if (analysisSummary.getAnalysisMinute() < analysisMinute) {
          getLogger().info("analysis for minute {} hasn't been found yet. Analysis found so far {}", analysisMinute,
              analysisSummary);
          sleep(ofMillis(5000));
          continue;
        }
        getLogger().info("found analysisSummary with message {}", analysisSummary.getAnalysisSummaryMessage());

        ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
        if (analysisSummary.getRiskLevel() == RiskLevel.HIGH) {
          getLogger().info(analysisSummary.getAnalysisSummaryMessage() + " Marking it failed.");
          executionStatus = ExecutionStatus.FAILED;
        } else if (analysisSummary.getRiskLevel() == RiskLevel.MEDIUM
            && getAnalysisTolerance().compareTo(AnalysisTolerance.MEDIUM) <= 0) {
          getLogger().info(analysisSummary.getAnalysisSummaryMessage() + " Marking it failed.");
          executionStatus = ExecutionStatus.FAILED;
        } else if (analysisSummary.getRiskLevel() == RiskLevel.LOW
            && getAnalysisTolerance().compareTo(AnalysisTolerance.LOW) == 0) {
          getLogger().info(analysisSummary.getAnalysisSummaryMessage() + " Marking it failed.");
          executionStatus = ExecutionStatus.FAILED;
        }

        getLogger().info("the final status is {}", executionStatus);
        executionResponse.getStateExecutionData().setStatus(executionStatus);
        continuousVerificationService.setMetaDataExecutionStatus(
            executionContext.getStateExecutionInstanceId(), executionStatus, false, false);
        return ExecutionResponse.builder()
            .executionStatus(isQAVerificationPath(context.getAccountId(), context.getAppId()) ? ExecutionStatus.SUCCESS
                                                                                              : executionStatus)
            .stateExecutionData(executionResponse.getStateExecutionData())
            .build();
      }

      executionResponse.getStateExecutionData().setErrorMsg(
          "Analysis for minute " + analysisMinute + " failed to save in DB");
      return ExecutionResponse.builder()
          .executionStatus(ExecutionStatus.ERROR)
          .stateExecutionData(executionResponse.getStateExecutionData())
          .build();
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext executionContext) {
    continuousVerificationService.setMetaDataExecutionStatus(
        executionContext.getStateExecutionInstanceId(), ExecutionStatus.ABORTED, true, false);
    AnalysisContext analysisContext =
        wingsPersistence.createQuery(AnalysisContext.class)
            .filter("appId", executionContext.getAppId())
            .filter(AnalysisContextKeys.stateExecutionId, executionContext.getStateExecutionInstanceId())
            .get();

    if (analysisContext == null) {
      analysisContext = getLogAnalysisContext(executionContext, UUID.randomUUID().toString());
    }

    final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
        analysisContext.getStateExecutionId(), analysisContext.getAppId(), StateType.valueOf(getStateType()));

    if (analysisSummary == null) {
      generateAnalysisResponse(analysisContext, ExecutionStatus.ABORTED, "Workflow was aborted while analysing");
    }

    if (isNotEmpty(analysisContext.getPredictiveCvConfigId())) {
      getLogger().info("disabling the predictive cv config {} state {}", analysisContext.getPredictiveCvConfigId(),
          analysisContext.getStateExecutionId());
      wingsPersistence.updateField(
          LogsCVConfiguration.class, analysisContext.getPredictiveCvConfigId(), "enabled24x7", false);
    }
  }

  protected ExecutionResponse generateAnalysisResponse(
      AnalysisContext context, ExecutionStatus status, String message) {
    analysisService.createAndSaveSummary(
        context.getStateType(), context.getAppId(), context.getStateExecutionId(), context.getQuery(), message);
    return createExecutionResponse(context, status, message, true);
  }

  private AnalysisContext getLogAnalysisContext(ExecutionContext context, String correlationId) {
    Map<String, String> controlNodes = new HashMap<>();
    Map<String, String> testNodes = new HashMap<>();
    if (isNewInstanceFieldPopulated(context)) {
      populateNewAndOldHostNames(context, controlNodes, testNodes);
    } else {
      controlNodes =
          getComparisonStrategy() == COMPARE_WITH_PREVIOUS ? Collections.emptyMap() : getLastExecutionNodes(context);
      testNodes = getCanaryNewHostNames(context);
    }
    testNodes.keySet().forEach(controlNodes::remove);
    renderedQuery = context.renderExpression(query);

    String accountId = this.appService.get(context.getAppId()).getAccountId();
    String hostNameField = getHostnameField(context);

    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(accountId)
            .appId(context.getAppId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .serviceId(getPhaseServiceId(context))
            .analysisType(MLAnalysisType.LOG_ML)
            .controlNodes(controlNodes)
            .testNodes(testNodes)
            .query(getRenderedQuery())
            .isSSL(this.configuration.isSslEnabled())
            .appPort(this.configuration.getApplicationPort())
            .comparisonStrategy(getComparisonStrategy())
            .timeDuration(Integer.parseInt(getTimeDuration()))
            .stateType(StateType.valueOf(getStateType()))
            .analysisServerConfigId(getAnalysisServerConfigId())
            .correlationId(correlationId)
            .managerVersion(versionInfoManager.getVersionInfo().getVersion())
            .envId(getEnvId(context))
            .hostNameField(hostNameField)
            .startDataCollectionMinute(TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()))
            .predictiveHistoryMinutes(Integer.parseInt(getPredictiveHistoryMinutes()))
            .build();

    // Saving data collection info as part of context.
    // This will be directly used as part of delegate task
    // todo: Pranjal: Condition will be removed once enabled for all verifiers.
    if (getComparisonStrategy() == PREDICTIVE
        || (featureFlagService.isEnabled(FeatureName.CV_DATA_COLLECTION_JOB, accountId)
               && (PER_MINUTE_CV_STATES.contains(StateType.valueOf(getStateType()))))
        || GA_PER_MINUTE_CV_STATES.contains(StateType.valueOf(getStateType()))) {
      DataCollectionInfo dataCollectionInfo = createDataCollectionInfo(analysisContext);
      analysisContext.setDataCollectionInfo(dataCollectionInfo);
    }
    sampleHostsMap(analysisContext);
    if (getCVTaskFeatureName().isPresent()) {
      analysisContext.setFeatureFlag(
          getCVTaskFeatureName().get(), isCVTaskEnqueuingEnabled(analysisContext.getAccountId()));
    }
    return analysisContext;
  }

  public String getHostnameField(ExecutionContext context) {
    switch (StateType.valueOf(getStateType())) {
      case SUMO:
        return ((SumoLogicAnalysisState) this).getHostnameField().getHostNameField();
      case ELK:
        return context.renderExpression(((ElkAnalysisState) this).getHostnameField());
      case DATA_DOG_LOG:
      case STACK_DRIVER_LOG:
        return this.getHostnameField(context);
      default:
        return null;
    }
  }

  public DataCollectionInfo createDataCollectionInfo(AnalysisContext analysisContext) {
    StateType stateType = StateType.valueOf(getStateType());
    switch (stateType) {
      case SUMO:
        SumoLogicAnalysisState sumoLogicAnalysisState = (SumoLogicAnalysisState) this;
        SumoConfig sumoConfig = (SumoConfig) settingsService.get(getAnalysisServerConfigId()).getValue();
        return SumoDataCollectionInfo.builder()
            .sumoConfig(sumoConfig)
            .accountId(analysisContext.getAccountId())
            .applicationId(analysisContext.getAppId())
            .stateExecutionId(analysisContext.getStateExecutionId())
            .workflowId(analysisContext.getWorkflowId())
            .workflowExecutionId(analysisContext.getWorkflowExecutionId())
            .serviceId(analysisContext.getServiceId())
            .query(sumoLogicAnalysisState.getRenderedQuery())
            .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
            .hostnameField(sumoLogicAnalysisState.getHostnameField().getHostNameField())
            .build();
      case DATA_DOG_LOG:
        DatadogLogState datadogLogState = (DatadogLogState) this;
        DatadogConfig datadogConfig = (DatadogConfig) settingsService.get(getAnalysisServerConfigId()).getValue();
        return CustomLogDataCollectionInfo.builder()
            .baseUrl(datadogConfig.getUrl())
            .validationUrl(DatadogConfig.validationUrl)
            .dataUrl(DatadogConfig.LOG_API_PATH_SUFFIX)
            .headers(new HashMap<>())
            .options(datadogConfig.fetchLogOptionsMap())
            .body(DatadogLogState.resolveHostnameField(
                datadogConfig.fetchLogBodyMap(false), analysisContext.getHostNameField()))
            .query(getRenderedQuery())
            .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
            .stateType(StateType.DATA_DOG_LOG)
            .applicationId(analysisContext.getAppId())
            .stateExecutionId(analysisContext.getStateExecutionId())
            .workflowId(analysisContext.getWorkflowId())
            .workflowExecutionId(analysisContext.getWorkflowExecutionId())
            .serviceId(analysisContext.getServiceId())
            .hostnameSeparator(DatadogLogState.HOST_NAME_SEPARATOR)
            .hostnameField(analysisContext.getHostNameField())
            .responseDefinition(
                datadogLogState.constructLogDefinitions(datadogConfig, analysisContext.getHostNameField(), false))
            .shouldInspectHosts(true)
            .collectionFrequency(1)
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .accountId(analysisContext.getAccountId())
            .build();
      case STACK_DRIVER_LOG:
        StackDriverLogState stackDriverLogState = (StackDriverLogState) this;
        GcpConfig gcpConfig = (GcpConfig) settingsService.get(getAnalysisServerConfigId()).getValue();
        return StackDriverLogDataCollectionInfo.builder()
            .gcpConfig(gcpConfig)
            .hostnameField(analysisContext.getHostNameField())
            .stateType(StateType.STACK_DRIVER_LOG)
            .applicationId(analysisContext.getAppId())
            .logMessageField(stackDriverLogState.getLogMessageField())
            .stateExecutionId(analysisContext.getStateExecutionId())
            .workflowId(analysisContext.getWorkflowId())
            .workflowExecutionId(analysisContext.getWorkflowExecutionId())
            .serviceId(analysisContext.getServiceId())
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
            .accountId(analysisContext.getAccountId())
            .query(getRenderedQuery())
            .initialDelayMinutes(0)
            .build();
      default:
        return null;
    }
  }

  public abstract AnalysisTolerance getAnalysisTolerance();

  public void setAnalysisTolerance(String tolerance) {
    this.tolerance = tolerance;
  }

  protected List<Set<String>> batchHosts(Set<String> hosts) {
    List<Set<String>> batchedHosts = new ArrayList<>();
    Set<String> batch = new HashSet<>();
    for (String host : hosts) {
      if (batch.size() == HOST_BATCH_SIZE) {
        batchedHosts.add(batch);
        batch = new HashSet<>();
      }
      batch.add(host);
    }
    if (!batch.isEmpty()) {
      batchedHosts.add(batch);
    }

    return batchedHosts;
  }

  private void generateDemoThirdPartyApiCallLogs(String accountId, String stateExecutionId, boolean failedState) {
    super.generateDemoThirdPartyApiCallLogs(
        accountId, stateExecutionId, failedState, demoRequestBody(), demoLogResponse());
  }
  private String demoRequestBody() {
    return DEMO_REQUEST_BODY;
  }
  private String demoLogResponse() {
    return DEMO_RESPONSE_BODY;
  }
}
