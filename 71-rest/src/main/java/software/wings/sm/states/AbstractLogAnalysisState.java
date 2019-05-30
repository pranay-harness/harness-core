package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_CURRENT;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.PREDICTIVE;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.ExceptionUtils;
import io.harness.time.Timestamp;
import io.harness.version.VersionInfoManager;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.DatadogConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.FeatureName;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionInfo;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;
import software.wings.verification.log.LogsCVConfiguration;

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

  protected String query;

  @Transient @Inject @SchemaIgnore protected AnalysisService analysisService;
  @Transient @Inject @SchemaIgnore protected VersionInfoManager versionInfoManager;
  @SchemaIgnore @Transient private String renderedQuery;

  public AbstractLogAnalysisState(String name, String stateType) {
    super(name, stateType);
  }

  @SchemaIgnore
  public static String getStateBaseUrl(StateType stateType) {
    switch (stateType) {
      case ELK:
        return LogAnalysisResource.ELK_RESOURCE_BASE_URL;
      case LOGZ:
        return LogAnalysisResource.LOGZ_RESOURCE_BASE_URL;
      case SPLUNKV2:
        return LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL;
      case SUMO:
        return LogAnalysisResource.SUMO_RESOURCE_BASE_URL;

      default:
        throw new IllegalArgumentException("invalid stateType: " + stateType);
    }
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
    String corelationId = UUID.randomUUID().toString();
    String delegateTaskId = null;
    try {
      renderedQuery = executionContext.renderExpression(query);
      getLogger().info("Executing {} state, id: {} ", getStateType(), executionContext.getStateExecutionInstanceId());
      cleanUpForRetry(executionContext);
      AnalysisContext analysisContext = getLogAnalysisContext(executionContext, corelationId);
      getLogger().info("id: {} context: {}", executionContext.getStateExecutionInstanceId(), analysisContext);
      saveMetaDataForDashboard(analysisContext.getAccountId(), executionContext);

      Set<String> canaryNewHostNames = analysisContext.getTestNodes().keySet();
      if (isDemoPath(analysisContext.getAccountId())) {
        if (settingsService.get(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("dev")
            || settingsService.get(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("prod")) {
          boolean failedState =
              settingsService.get(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("dev");
          if (failedState) {
            return generateAnalysisResponse(analysisContext, ExecutionStatus.FAILED, "Demo CV");
          } else {
            return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS, "Demo CV");
          }
        }
      }

      if (isEmpty(canaryNewHostNames)) {
        getLogger().warn(
            "id: {}, Could not find test nodes to compare the data", executionContext.getStateExecutionInstanceId());
        return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS, "Could not find hosts to analyze!");
      }

      Set<String> lastExecutionNodes = analysisContext.getControlNodes().keySet();
      if (isEmpty(lastExecutionNodes)) {
        if (getComparisonStrategy() == COMPARE_WITH_CURRENT) {
          getLogger().info("id: {}, No nodes with older version found to compare the logs. Skipping analysis",
              executionContext.getStateExecutionInstanceId());
          return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS,
              "Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.");
        }

        getLogger().warn(
            "id: {}, It seems that there is no successful run for this workflow yet. Log data will be collected to be analyzed for next deployment run",
            executionContext.getStateExecutionInstanceId());
      }

      String responseMessage = "Log Verification running.";
      if (getComparisonStrategy() == COMPARE_WITH_PREVIOUS) {
        WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
        String baselineWorkflowExecutionId = workflowExecutionBaselineService.getBaselineExecutionId(
            analysisContext.getAppId(), analysisContext.getWorkflowId(), workflowStandardParams.getEnv().getUuid(),
            analysisContext.getServiceId());
        if (isEmpty(baselineWorkflowExecutionId)) {
          responseMessage = "No baseline was set for the workflow. Workflow running with auto baseline.";
          getLogger().info("id: {}, {}", executionContext.getStateExecutionInstanceId(), responseMessage);
          baselineWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
              analysisContext.getStateExecutionId(), analysisContext.getStateType(), analysisContext.getAppId(),
              analysisContext.getServiceId(), analysisContext.getWorkflowId(), analysisContext.getQuery());
        } else {
          responseMessage = "Baseline is pinned for the workflow. Analyzing against pinned baseline.";
          getLogger().info("Baseline is pinned for stateExecution: {}, baselineId: ",
              analysisContext.getStateExecutionId(), baselineWorkflowExecutionId);
        }
        if (baselineWorkflowExecutionId == null) {
          responseMessage += " No previous execution found. This will be the baseline run.";
          getLogger().warn("id: {}, No previous execution found. This will be the baseline run",
              executionContext.getStateExecutionInstanceId());
        }
        getLogger().info(
            "Baseline execution for {} is {}", analysisContext.getStateExecutionId(), baselineWorkflowExecutionId);
        analysisContext.setPrevWorkflowExecutionId(baselineWorkflowExecutionId);
      }

      final VerificationStateAnalysisExecutionData executionData =
          VerificationStateAnalysisExecutionData.builder()
              .appId(analysisContext.getAppId())
              .serviceId(getPhaseServiceId(executionContext))
              .stateExecutionInstanceId(analysisContext.getStateExecutionId())
              .serverConfigId(getAnalysisServerConfigId())
              .query(getRenderedQuery())
              .timeDuration(Integer.parseInt(getTimeDuration()))
              .canaryNewHostNames(canaryNewHostNames)
              .lastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : new HashSet<>(lastExecutionNodes))
              .correlationId(analysisContext.getCorrelationId())
              .mlAnalysisType(MLAnalysisType.LOG_ML)
              .build();

      executionData.setStatus(ExecutionStatus.RUNNING);
      executionData.setErrorMsg(responseMessage);

      Set<String> hostsToBeCollected = new HashSet<>();
      if (getComparisonStrategy() == COMPARE_WITH_CURRENT && lastExecutionNodes != null) {
        hostsToBeCollected.addAll(lastExecutionNodes);
      }

      hostsToBeCollected.addAll(canaryNewHostNames);
      hostsToBeCollected.remove(null);
      getLogger().info("triggering data collection for {} state, id: {} ", getStateType(),
          executionContext.getStateExecutionInstanceId());

      // In case of predictive the data collection will be handled as per 24x7 logic
      // Or in case when feature flag CV_DATA_COLLECTION_JOB is enabled. Delegate task creation will be every minute
      if (getComparisonStrategy() == PREDICTIVE
          || (featureFlagService.isEnabled(FeatureName.CV_DATA_COLLECTION_JOB, executionContext.getAccountId())
                     && (getStateType().equals(StateType.SUMO.name()))
                 || getStateType().equals(StateType.DATA_DOG_LOG.name()))) {
        getLogger().info(
            "Feature flag CV_DATA_COLLECTION_JOB is enabled will use data collection for triggering delegate task");
      } else {
        delegateTaskId = triggerAnalysisDataCollection(executionContext, executionData, hostsToBeCollected);
        getLogger().info("triggered data collection for {} state, id: {}, delgateTaskId: {}", getStateType(),
            executionContext.getStateExecutionInstanceId(), delegateTaskId);
      }
      // Set the rendered query into the analysis context which will be used during task analysis.
      analysisContext.setQuery(getRenderedQuery());

      scheduleAnalysisCronJob(analysisContext, delegateTaskId);

      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(Collections.singletonList(analysisContext.getCorrelationId()))
          .withExecutionStatus(ExecutionStatus.RUNNING)
          .withErrorMessage(responseMessage)
          .withStateExecutionData(executionData)
          .withDelegateTaskId(delegateTaskId)
          .build();
    } catch (Exception ex) {
      getLogger().error("log analysis state failed ", ex);
      // set the CV Metadata status to ERROR as well.
      continuousVerificationService.setMetaDataExecutionStatus(
          executionContext.getStateExecutionInstanceId(), ExecutionStatus.ERROR, true);
      return anExecutionResponse()
          .withAsync(false)
          .withCorrelationIds(Collections.singletonList(corelationId))
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withErrorMessage(ExceptionUtils.getMessage(ex))
          .withStateExecutionData(VerificationStateAnalysisExecutionData.builder()
                                      .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                      .serverConfigId(getAnalysisServerConfigId())
                                      .query(getRenderedQuery())
                                      .timeDuration(Integer.parseInt(getTimeDuration()))
                                      .delegateTaskId(delegateTaskId)
                                      .build())
          .build();
    }
  }

  protected abstract String triggerAnalysisDataCollection(
      ExecutionContext context, VerificationStateAnalysisExecutionData executionData, Set<String> hosts);

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext executionContext, Map<String, ResponseData> response) {
    VerificationDataAnalysisResponse executionResponse =
        (VerificationDataAnalysisResponse) response.values().iterator().next();

    if (ExecutionStatus.isBrokeStatus(executionResponse.getExecutionStatus())) {
      getLogger().info(
          "for {} got failed execution response {}", executionContext.getStateExecutionInstanceId(), executionResponse);
      continuousVerificationService.setMetaDataExecutionStatus(
          executionContext.getStateExecutionInstanceId(), ExecutionStatus.ERROR, true);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withStateExecutionData(executionResponse.getStateExecutionData())
          .withErrorMessage(executionResponse.getStateExecutionData().getErrorMsg())
          .build();
    } else {
      AnalysisContext context =
          getLogAnalysisContext(executionContext, executionResponse.getStateExecutionData().getCorrelationId());
      int analysisMinute = executionResponse.getStateExecutionData().getAnalysisMinute();
      for (int i = 0; i < NUM_OF_RETRIES; i++) {
        final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
            context.getStateExecutionId(), context.getAppId(), StateType.valueOf(getStateType()));

        if (analysisSummary == null) {
          getLogger().info("for {} No analysis summary. This can happen if there is no data with the given queries",
              context.getStateExecutionId());
          continuousVerificationService.setMetaDataExecutionStatus(
              executionContext.getStateExecutionInstanceId(), ExecutionStatus.SUCCESS, true);
          return isQAVerificationPath(context.getAccountId(), context.getAppId())
              ? generateAnalysisResponse(context, ExecutionStatus.FAILED, "No Analysis result found")
              : generateAnalysisResponse(
                    context, ExecutionStatus.SUCCESS, "No data found with given queries. Skipped Analysis");
        }

        if (analysisSummary.getAnalysisMinute() < analysisMinute) {
          getLogger().info("for {} analysis for minute {} hasn't been found yet. Analysis found so far {}",
              context.getStateExecutionId(), analysisMinute, analysisSummary);
          sleep(ofMillis(5000));
          continue;
        }
        getLogger().info("for {} found analysisSummary with message {}", context.getStateExecutionId(),
            analysisSummary.getAnalysisSummaryMessage());

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

        getLogger().info("for {} the final status is {}", context.getStateExecutionId(), executionStatus);
        executionResponse.getStateExecutionData().setStatus(executionStatus);
        continuousVerificationService.setMetaDataExecutionStatus(
            executionContext.getStateExecutionInstanceId(), executionStatus, false);
        return anExecutionResponse()
            .withExecutionStatus(isQAVerificationPath(context.getAccountId(), context.getAppId())
                    ? ExecutionStatus.SUCCESS
                    : executionStatus)
            .withStateExecutionData(executionResponse.getStateExecutionData())
            .build();
      }

      executionResponse.getStateExecutionData().setErrorMsg(
          "Analysis for minute " + analysisMinute + " failed to save in DB");
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withStateExecutionData(executionResponse.getStateExecutionData())
          .build();
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext executionContext) {
    continuousVerificationService.setMetaDataExecutionStatus(
        executionContext.getStateExecutionInstanceId(), ExecutionStatus.ABORTED, true);
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

    VerificationStateAnalysisExecutionData executionData = VerificationStateAnalysisExecutionData.builder()
                                                               .stateExecutionInstanceId(context.getStateExecutionId())
                                                               .serverConfigId(context.getAnalysisServerConfigId())
                                                               .query(context.getQuery())
                                                               .timeDuration(context.getTimeDuration())
                                                               .correlationId(context.getCorrelationId())
                                                               .build();
    executionData.setStatus(status);
    continuousVerificationService.setMetaDataExecutionStatus(context.getStateExecutionId(), status, true);
    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }

  private AnalysisContext getLogAnalysisContext(ExecutionContext context, String correlationId) {
    Map<String, String> controlNodes =
        getComparisonStrategy() == COMPARE_WITH_PREVIOUS ? Collections.emptyMap() : getLastExecutionNodes(context);
    Map<String, String> testNodes = getCanaryNewHostNames(context);
    testNodes.keySet().forEach(testNode -> controlNodes.remove(testNode));
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    String hostNameField = getHostnameField(context);

    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(this.appService.get(context.getAppId()).getAccountId())
            .appId(context.getAppId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .serviceId(getPhaseServiceId(context))
            .analysisType(MLAnalysisType.LOG_ML)
            .controlNodes(controlNodes)
            .testNodes(testNodes)
            .query(query)
            .isSSL(this.configuration.isSslEnabled())
            .appPort(this.configuration.getApplicationPort())
            .comparisonStrategy(getComparisonStrategy())
            .timeDuration(Integer.parseInt(getTimeDuration()))
            .stateType(StateType.valueOf(getStateType()))
            .analysisServerConfigId(getAnalysisServerConfigId())
            .correlationId(correlationId)
            .managerVersion(versionInfoManager.getVersionInfo().getVersion())
            .envId(envId)
            .hostNameField(hostNameField)
            .startDataCollectionMinute(TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()))
            .predictiveHistoryMinutes(Integer.parseInt(getPredictiveHistoryMinutes()))
            .build();

    // Data collection info is created and put in Analysis context for persistence
    if (getComparisonStrategy().equals(PREDICTIVE)) {
      DataCollectionInfo dataCollectionInfo = createDataCollectionInfo(analysisContext);
      analysisContext.setDataCollectionInfo(dataCollectionInfo);
    }
    return analysisContext;
  }

  public String getHostnameField(ExecutionContext context) {
    switch (StateType.valueOf(getStateType())) {
      case SUMO:
        return ((SumoLogicAnalysisState) this).getHostnameField().getHostNameField();
      case DATA_DOG_LOG:
        return this.getHostnameField(context);
      default:
        return null;
    }
  }

  public DataCollectionInfo createDataCollectionInfo(AnalysisContext analysisContext) {
    StateType stateType = StateType.valueOf(getStateType());
    switch (stateType) {
      case ELK:
        ElkAnalysisState elkAnalysisState = (ElkAnalysisState) this;
        ElkConfig elkConfig = (ElkConfig) settingsService.get(getAnalysisServerConfigId()).getValue();
        return ElkDataCollectionInfo.builder()
            .elkConfig(elkConfig)
            .accountId(analysisContext.getAccountId())
            .applicationId(analysisContext.getAppId())
            .stateExecutionId(analysisContext.getStateExecutionId())
            .serviceId(analysisContext.getServiceId())
            .query(elkAnalysisState.getQuery())
            .indices(elkAnalysisState.getIndices())
            .hostnameField(elkAnalysisState.getHostnameField())
            .messageField(elkAnalysisState.getMessageField())
            .timestampField(elkAnalysisState.getTimestampField())
            .timestampFieldFormat(elkAnalysisState.getTimestampFormat())
            .queryType(elkAnalysisState.getQueryType())
            .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
            .encryptedDataDetails(secretManager.getEncryptionDetails(elkConfig, analysisContext.getAppId(), null))
            .build();
      case DATA_DOG_LOG:
        DatadogLogState datadogLogState = (DatadogLogState) this;
        DatadogConfig datadogConfig = (DatadogConfig) settingsService.get(getAnalysisServerConfigId()).getValue();
        return CustomLogDataCollectionInfo.builder()
            .baseUrl(datadogConfig.getUrl())
            .validationUrl(DatadogConfig.validationUrl)
            .dataUrl(DatadogConfig.logAnalysisUrl)
            .headers(new HashMap<>())
            .options(datadogConfig.fetchLogOptionsMap())
            .body(DatadogLogState.resolveHostnameField(
                datadogConfig.fetchLogBodyMap(false), analysisContext.getHostNameField()))
            .query(getRenderedQuery())
            .encryptedDataDetails(secretManager.getEncryptionDetails(
                datadogConfig, analysisContext.getAppId(), analysisContext.getWorkflowExecutionId()))
            .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
            .stateType(StateType.DATA_DOG_LOG)
            .applicationId(analysisContext.getAppId())
            .stateExecutionId(analysisContext.getStateExecutionId())
            .workflowId(analysisContext.getWorkflowId())
            .workflowExecutionId(analysisContext.getWorkflowExecutionId())
            .serviceId(analysisContext.getServiceId())
            .hostnameSeparator(DatadogLogState.hostNameSeparator)
            .hostnameField(analysisContext.getHostNameField())
            .responseDefinition(
                datadogLogState.constructLogDefinitions(datadogConfig, analysisContext.getHostNameField(), false))
            .shouldInspectHosts(true)
            .collectionFrequency(1)
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .accountId(analysisContext.getAccountId())
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
}
