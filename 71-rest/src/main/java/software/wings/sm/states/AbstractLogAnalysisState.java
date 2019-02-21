package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.ExceptionUtils;
import io.harness.time.Timestamp;
import io.harness.version.VersionInfoManager;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.FeatureName;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;

import java.util.ArrayList;
import java.util.Collections;
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
        if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
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
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS) {
        WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
        String baselineWorkflowExecutionId = workflowExecutionBaselineService.getBaselineExecutionId(
            analysisContext.getAppId(), analysisContext.getWorkflowId(), workflowStandardParams.getEnv().getUuid(),
            analysisContext.getServiceId());
        if (isEmpty(baselineWorkflowExecutionId)) {
          responseMessage = "No baseline was set for the workflow. Workflow running with auto baseline.";
          getLogger().info("id: {}, {}", executionContext.getStateExecutionInstanceId(), responseMessage);
          baselineWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
              analysisContext.getStateType(), analysisContext.getAppId(), analysisContext.getServiceId(),
              analysisContext.getWorkflowId(), analysisContext.getQuery());
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

      final LogAnalysisExecutionData executionData =
          LogAnalysisExecutionData.builder()
              .stateExecutionInstanceId(analysisContext.getStateExecutionId())
              .serverConfigId(getAnalysisServerConfigId())
              .query(getRenderedQuery())
              .timeDuration(Integer.parseInt(getTimeDuration()))
              .canaryNewHostNames(canaryNewHostNames)
              .lastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : new HashSet<>(lastExecutionNodes))
              .correlationId(analysisContext.getCorrelationId())
              .build();

      executionData.setStatus(ExecutionStatus.RUNNING);
      executionData.setErrorMsg(responseMessage);

      Set<String> hostsToBeCollected = new HashSet<>();
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT && lastExecutionNodes != null) {
        hostsToBeCollected.addAll(lastExecutionNodes);
      }

      hostsToBeCollected.addAll(canaryNewHostNames);
      hostsToBeCollected.remove(null);
      getLogger().info("triggering data collection for {} state, id: {} ", getStateType(),
          executionContext.getStateExecutionInstanceId());

      // Currently only SUMO implemented for creating separate data collection job each minute
      if (featureFlagService.isEnabled(FeatureName.CV_DATA_COLLECTION_JOB, executionContext.getAccountId())
          && getStateType().equals(StateType.SUMO.name())) {
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
          executionContext.getStateExecutionInstanceId(), ExecutionStatus.ERROR);
      return anExecutionResponse()
          .withAsync(false)
          .withCorrelationIds(Collections.singletonList(corelationId))
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withErrorMessage(ExceptionUtils.getMessage(ex))
          .withStateExecutionData(LogAnalysisExecutionData.builder()
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
      ExecutionContext context, LogAnalysisExecutionData executionData, Set<String> hosts);

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext executionContext, Map<String, ResponseData> response) {
    LogAnalysisResponse executionResponse = (LogAnalysisResponse) response.values().iterator().next();

    if (ExecutionStatus.isBrokeStatus(executionResponse.getExecutionStatus())) {
      getLogger().info(
          "for {} got failed execution response {}", executionContext.getStateExecutionInstanceId(), executionResponse);
      continuousVerificationService.setMetaDataExecutionStatus(
          executionContext.getStateExecutionInstanceId(), ExecutionStatus.FAILED);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withStateExecutionData(executionResponse.getLogAnalysisExecutionData())
          .withErrorMessage(executionResponse.getLogAnalysisExecutionData().getErrorMsg())
          .build();
    } else {
      AnalysisContext context =
          getLogAnalysisContext(executionContext, executionResponse.getLogAnalysisExecutionData().getCorrelationId());
      int analysisMinute = executionResponse.getLogAnalysisExecutionData().getAnalysisMinute();
      for (int i = 0; i < NUM_OF_RETRIES; i++) {
        final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
            context.getStateExecutionId(), context.getAppId(), StateType.valueOf(getStateType()));

        if (analysisSummary == null
            || (analysisSummary.isEmptyResult() && analysisSummary.getAnalysisMinute() >= analysisMinute)) {
          getLogger().info("for {} No analysis summary. This can happen if there is no data with the given queries",
              context.getStateExecutionId());
          continuousVerificationService.setMetaDataExecutionStatus(
              executionContext.getStateExecutionInstanceId(), ExecutionStatus.SUCCESS);
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
        executionResponse.getLogAnalysisExecutionData().setStatus(executionStatus);
        continuousVerificationService.setMetaDataExecutionStatus(
            executionContext.getStateExecutionInstanceId(), executionStatus);
        return anExecutionResponse()
            .withExecutionStatus(isQAVerificationPath(context.getAccountId(), context.getAppId())
                    ? ExecutionStatus.SUCCESS
                    : executionStatus)
            .withStateExecutionData(executionResponse.getLogAnalysisExecutionData())
            .build();
      }

      executionResponse.getLogAnalysisExecutionData().setErrorMsg(
          "Analysis for minute " + analysisMinute + " failed to save in DB");
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withStateExecutionData(executionResponse.getLogAnalysisExecutionData())
          .build();
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext executionContext) {
    continuousVerificationService.setMetaDataExecutionStatus(
        executionContext.getStateExecutionInstanceId(), ExecutionStatus.ABORTED);
    AnalysisContext context = getLogAnalysisContext(executionContext, UUID.randomUUID().toString());

    final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
        context.getStateExecutionId(), context.getAppId(), StateType.valueOf(getStateType()));

    if (analysisSummary == null) {
      generateAnalysisResponse(context, ExecutionStatus.ABORTED, "Workflow was aborted while analysing");
    }
  }

  protected ExecutionResponse generateAnalysisResponse(
      AnalysisContext context, ExecutionStatus status, String message) {
    analysisService.createAndSaveSummary(
        context.getStateType(), context.getAppId(), context.getStateExecutionId(), context.getQuery(), message);

    LogAnalysisExecutionData executionData = LogAnalysisExecutionData.builder()
                                                 .stateExecutionInstanceId(context.getStateExecutionId())
                                                 .serverConfigId(context.getAnalysisServerConfigId())
                                                 .query(context.getQuery())
                                                 .timeDuration(context.getTimeDuration())
                                                 .correlationId(context.getCorrelationId())
                                                 .build();
    executionData.setStatus(status);
    continuousVerificationService.setMetaDataExecutionStatus(context.getStateExecutionId(), status);
    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }

  private AnalysisContext getLogAnalysisContext(ExecutionContext context, String correlationId) {
    Map<String, String> controlNodes = getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS
        ? Collections.emptyMap()
        : getLastExecutionNodes(context);
    Map<String, String> testNodes = getCanaryNewHostNames(context);
    testNodes.keySet().forEach(testNode -> controlNodes.remove(testNode));
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    String hostNameField = null;
    if (StateType.valueOf(getStateType()).equals(StateType.SUMO)) {
      hostNameField = ((SumoLogicAnalysisState) this).getHostnameField();
    }
    return AnalysisContext.builder()
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
        .build();
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
