package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.common.VerificationConstants.GA_PER_MINUTE_CV_STATES;
import static software.wings.common.VerificationConstants.LAMBDA_HOST_NAME;
import static software.wings.common.VerificationConstants.PER_MINUTE_CV_STATES;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.PREDICTIVE;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.service.impl.security.SecretManagementDelegateServiceImpl.NUM_OF_RETRIES;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.time.Timestamp;
import io.harness.version.VersionInfoManager;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.PcfInstanceElement;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpConfig;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.DataCollectionInfo;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.stackdriver.StackDriverDataCollectionInfo;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    if (!checkLicense(appService.getAccountIdByAppId(context.getAppId()), StateType.valueOf(getStateType()),
            context.getStateExecutionInstanceId())) {
      return generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
          "Your license type does not support running this verification. Skipping Analysis");
    }
    String corelationId = UUID.randomUUID().toString();
    String delegateTaskId = null;
    VerificationStateAnalysisExecutionData executionData;
    try {
      getLogger().info("Executing {} state, id: {} ", getStateType(), context.getStateExecutionInstanceId());
      cleanUpForRetry(context);
      AnalysisContext analysisContext = getAnalysisContext(context, corelationId);
      getLogger().info("id: {} context: {}", context.getStateExecutionInstanceId(), analysisContext);
      saveMetaDataForDashboard(analysisContext.getAccountId(), context);

      if (isDemoPath(analysisContext.getAccountId())) {
        if (settingsService.get(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("dev")
            || settingsService.get(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("prod")) {
          boolean failedState =
              settingsService.get(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("dev");
          if (failedState) {
            return generateAnalysisResponse(context, ExecutionStatus.FAILED, "Demo CV");
          } else {
            return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, "Demo CV");
          }
        }
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
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, "Could not find nodes to analyze!");
      }

      Map<String, String> lastExecutionNodes = analysisContext.getControlNodes();
      if (isEmpty(lastExecutionNodes) && !isAwsLambdaState(context)) {
        if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
          getLogger().info("id: {}, No nodes with older version found to compare the logs. Skipping analysis",
              context.getStateExecutionInstanceId());
          return generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
              "Skipping analysis due to lack of baseline data (First time deployment or Last phase).");
        }

        getLogger().info(
            "id: {}, It seems that there is no successful run for this workflow yet. Metric data will be collected to be analyzed for next deployment run",
            context.getStateExecutionInstanceId());
      }

      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
          && lastExecutionNodes.equals(canaryNewHostNames)) {
        getLogger().warn("id: {} Control and test nodes are same. Will not be running Log analysis",
            context.getStateExecutionInstanceId());
        return generateAnalysisResponse(context, ExecutionStatus.FAILED,
            "Skipping analysis due to lack of baseline data (Minimum two phases are required).");
      }

      String responseMessage = "Metric Verification running";
      if (getComparisonStrategy() == COMPARE_WITH_PREVIOUS) {
        WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
        String baselineWorkflowExecutionId = workflowExecutionBaselineService.getBaselineExecutionId(context.getAppId(),
            context.getWorkflowId(), workflowStandardParams.getEnv().getUuid(), analysisContext.getServiceId());
        if (isEmpty(baselineWorkflowExecutionId)) {
          responseMessage = "No baseline was set for the workflow. Workflow running with auto baseline.";
          getLogger().info(responseMessage);
          baselineWorkflowExecutionId = metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
              analysisContext.getStateType(), analysisContext.getAppId(), analysisContext.getWorkflowId(),
              analysisContext.getServiceId(), getPhaseInfraMappingId(context));
        } else {
          responseMessage = "Baseline is fixed for the workflow. Analyzing against fixed baseline.";
          getLogger().info(
              "Baseline execution for {} is {}", analysisContext.getStateExecutionId(), baselineWorkflowExecutionId);
        }
        if (baselineWorkflowExecutionId == null) {
          responseMessage += " No previous execution found. This will be the baseline run";
          getLogger().warn("No previous execution found. This will be the baseline run");
        }
        analysisContext.setPrevWorkflowExecutionId(baselineWorkflowExecutionId);
      }

      int timeDurationInt = Integer.parseInt(getTimeDuration());
      executionData =
          VerificationStateAnalysisExecutionData.builder()
              .appId(context.getAppId())
              .serviceId(getPhaseServiceId(context))
              .workflowExecutionId(context.getWorkflowExecutionId())
              .stateExecutionInstanceId(context.getStateExecutionInstanceId())
              .serverConfigId(getAnalysisServerConfigId())
              .timeDuration(timeDurationInt)
              .canaryNewHostNames(canaryNewHostNames.keySet())
              .lastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : lastExecutionNodes.keySet())
              .correlationId(analysisContext.getCorrelationId())
              .canaryNewHostNames(analysisContext.getTestNodes().keySet())
              .lastExecutionNodes(analysisContext.getControlNodes().keySet())
              .mlAnalysisType(MLAnalysisType.TIME_SERIES)
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

      getLogger().info(
          "triggering data collection for {} state, id: {} ", getStateType(), context.getStateExecutionInstanceId());
      hostsToCollect.remove(null);
      createAndSaveMetricGroups(context, hostsToCollect);

      if (getComparisonStrategy() == PREDICTIVE
          || (featureFlagService.isEnabled(FeatureName.CV_DATA_COLLECTION_JOB, context.getAccountId())
                 && (PER_MINUTE_CV_STATES.contains(StateType.valueOf(getStateType()))))
          || GA_PER_MINUTE_CV_STATES.contains(StateType.valueOf(getStateType()))) {
        getLogger().info("Per Minute data collection will be done for triggering delegate task");
      } else {
        delegateTaskId = triggerAnalysisDataCollection(context, analysisContext, executionData, hostsToCollect);
        getLogger().info("triggered data collection for {} state, id: {}, delgateTaskId: {}", getStateType(),
            context.getStateExecutionInstanceId(), delegateTaskId);
      }

      executionData.setDelegateTaskId(delegateTaskId);
      final VerificationDataAnalysisResponse response =
          VerificationDataAnalysisResponse.builder().stateExecutionData(executionData).build();
      response.setExecutionStatus(ExecutionStatus.RUNNING);
      scheduleAnalysisCronJob(analysisContext, delegateTaskId);
      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
          .withExecutionStatus(ExecutionStatus.RUNNING)
          .withErrorMessage(responseMessage)
          .withStateExecutionData(executionData)
          .build();
    } catch (Exception ex) {
      // set the CV Metadata status to ERROR as well.
      continuousVerificationService.setMetaDataExecutionStatus(
          context.getStateExecutionInstanceId(), ExecutionStatus.ERROR, true);
      if (ex instanceof WingsException) {
        ExceptionLogger.logProcessedMessages((WingsException) ex, MANAGER, getLogger());
      } else {
        getLogger().error("metric analysis state failed", ex);
      }
      return anExecutionResponse()
          .withAsync(false)
          .withCorrelationIds(Collections.singletonList(corelationId))
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withErrorMessage(ExceptionUtils.getMessage(ex))
          .withStateExecutionData(VerificationStateAnalysisExecutionData.builder()
                                      .appId(context.getAppId())
                                      .workflowExecutionId(context.getWorkflowExecutionId())
                                      .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                                      .delegateTaskId(delegateTaskId)
                                      .serverConfigId(getAnalysisServerConfigId())
                                      .mlAnalysisType(MLAnalysisType.TIME_SERIES)
                                      .build())
          .build();
    }
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
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    VerificationDataAnalysisResponse executionResponse =
        (VerificationDataAnalysisResponse) response.values().iterator().next();

    if (ExecutionStatus.isBrokeStatus(executionResponse.getExecutionStatus())) {
      getLogger().info(
          "for {} got failed execution response {}", context.getStateExecutionInstanceId(), executionResponse);
      continuousVerificationService.setMetaDataExecutionStatus(
          context.getStateExecutionInstanceId(), ExecutionStatus.ERROR, true);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withStateExecutionData(executionResponse.getStateExecutionData())
          .withErrorMessage(executionResponse.getStateExecutionData().getErrorMsg())
          .build();
    }

    int analysisMinute = executionResponse.getStateExecutionData().getAnalysisMinute();
    for (int i = 0; i < NUM_OF_RETRIES; i++) {
      Set<NewRelicMetricAnalysisRecord> metricAnalysisRecords = metricAnalysisService.getMetricsAnalysis(
          context.getAppId(), context.getStateExecutionInstanceId(), context.getWorkflowExecutionId());
      if (isEmpty(metricAnalysisRecords)) {
        getLogger().info("for {} No analysis summary.", context.getStateExecutionInstanceId());
        continuousVerificationService.setMetaDataExecutionStatus(
            context.getStateExecutionInstanceId(), ExecutionStatus.SUCCESS, true);
        return isQAVerificationPath(this.appService.get(context.getAppId()).getAccountId(), context.getAppId())
            ? generateAnalysisResponse(context, ExecutionStatus.FAILED, "No Analysis result found")
            : generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
                  "No data found for comparison. Please check load. Skipping analysis.");
      }

      boolean analysisFound = false;
      for (NewRelicMetricAnalysisRecord analysisRecord : metricAnalysisRecords) {
        if (analysisRecord.getAnalysisMinute() >= analysisMinute) {
          analysisFound = true;
          break;
        }
      }

      if (!analysisFound) {
        getLogger().info("for {} analysis for minute {} hasn't been found yet. Analysis found so far {}",
            context.getStateExecutionInstanceId(), analysisMinute, metricAnalysisRecords);
        sleep(ofMillis(5000));
        continue;
      }

      if (isQAVerificationPath(appService.get(context.getAppId()).getAccountId(), context.getAppId())) {
        boolean isResultPresent = false;
        for (NewRelicMetricAnalysisRecord metricAnalysisRecord : metricAnalysisRecords) {
          if (isNotEmpty(metricAnalysisRecord.getMetricAnalyses())) {
            isResultPresent = true;
            break;
          }
        }
        if (!isResultPresent) {
          continuousVerificationService.setMetaDataExecutionStatus(
              executionResponse.getStateExecutionData().getStateExecutionInstanceId(), ExecutionStatus.FAILED, true);
          return anExecutionResponse()
              .withExecutionStatus(ExecutionStatus.FAILED)
              .withStateExecutionData(executionResponse.getStateExecutionData())
              .build();
        }
      }
      getLogger().info("for {} found analysisSummary with analysis records {}", context.getStateExecutionInstanceId(),
          metricAnalysisRecords.size());
      for (NewRelicMetricAnalysisRecord metricAnalysisRecord : metricAnalysisRecords) {
        if (metricAnalysisRecord.getRiskLevel() == RiskLevel.HIGH) {
          executionStatus = ExecutionStatus.FAILED;
        }
      }
      executionResponse.getStateExecutionData().setStatus(executionStatus);
      getLogger().info("State done with status {}, id: {}", executionStatus, context.getStateExecutionInstanceId());
      continuousVerificationService.setMetaDataExecutionStatus(
          context.getStateExecutionInstanceId(), executionStatus, false);

      metricAnalysisService.saveRawDataToGoogleDataStore(
          context.getAccountId(), context.getStateExecutionInstanceId(), executionStatus, getPhaseServiceId(context));

      return anExecutionResponse()
          .withExecutionStatus(
              isQAVerificationPath(appService.get(context.getAppId()).getAccountId(), context.getAppId())
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

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    continuousVerificationService.setMetaDataExecutionStatus(
        context.getStateExecutionInstanceId(), ExecutionStatus.ABORTED, true);
  }

  protected ExecutionResponse generateAnalysisResponse(
      ExecutionContext context, ExecutionStatus status, String message) {
    final VerificationStateAnalysisExecutionData executionData =
        VerificationStateAnalysisExecutionData.builder()
            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
            .serverConfigId(getAnalysisServerConfigId())
            .timeDuration(Integer.parseInt(getTimeDuration()))
            .correlationId(UUID.randomUUID().toString())
            .build();
    executionData.setStatus(status);
    NewRelicMetricAnalysisRecord metricAnalysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                            .message(message)
                                                            .appId(context.getAppId())
                                                            .stateType(StateType.valueOf(getStateType()))
                                                            .stateExecutionId(context.getStateExecutionInstanceId())
                                                            .workflowExecutionId(context.getWorkflowExecutionId())
                                                            .build();

    wingsPersistence.saveIgnoringDuplicateKeys(Lists.newArrayList(metricAnalysisRecord));
    continuousVerificationService.setMetaDataExecutionStatus(context.getStateExecutionInstanceId(), status, true);

    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }

  private AnalysisContext getAnalysisContext(ExecutionContext context, String correlationId) {
    Map<String, String> controlNodes =
        getComparisonStrategy() == COMPARE_WITH_PREVIOUS ? Collections.emptyMap() : getLastExecutionNodes(context);
    Map<String, String> testNodes = getCanaryNewHostNames(context);
    testNodes.keySet().forEach(testNode -> controlNodes.remove(testNode));
    int timeDurationInt = Integer.parseInt(getTimeDuration());
    String accountId = this.appService.get(context.getAppId()).getAccountId();

    AnalysisContext analysisContext =
        AnalysisContext.builder()
            .accountId(this.appService.get(context.getAppId()).getAccountId())
            .appId(context.getAppId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .serviceId(getPhaseServiceId(context))
            .analysisType(MLAnalysisType.TIME_SERIES)
            .controlNodes(controlNodes)
            .testNodes(testNodes)
            .isSSL(this.configuration.isSslEnabled())
            .appPort(this.configuration.getApplicationPort())
            .comparisonStrategy(getComparisonStrategy())
            .timeDuration(timeDurationInt)
            .stateType(StateType.valueOf(getStateType()))
            .analysisServerConfigId(getAnalysisServerConfigId())
            .correlationId(correlationId)
            .smooth_window(SMOOTH_WINDOW)
            .tolerance(getAnalysisTolerance().tolerance())
            .minimumRequestsPerMinute(MIN_REQUESTS_PER_MINUTE)
            .comparisonWindow(COMPARISON_WINDOW)
            .startDataCollectionMinute(TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary()))
            .parallelProcesses(PARALLEL_PROCESSES)
            .managerVersion(versionInfoManager.getVersionInfo().getVersion())
            .build();

    if (getComparisonStrategy() == PREDICTIVE
        || (featureFlagService.isEnabled(FeatureName.CV_DATA_COLLECTION_JOB, accountId)
               && (PER_MINUTE_CV_STATES.contains(StateType.valueOf(getStateType()))))
        || GA_PER_MINUTE_CV_STATES.contains(StateType.valueOf(getStateType()))) {
      DataCollectionInfo dataCollectionInfo = createDataCollectionInfo(context);
      analysisContext.setDataCollectionInfo(dataCollectionInfo);
    }
    return analysisContext;
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
            .hosts(Maps.newHashMap())
            .loadBalancerMetrics(stackDriverState.fetchLoadBalancerMetrics())
            .podMetrics(stackDriverState.fetchPodMetrics())
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

  public abstract AnalysisTolerance getAnalysisTolerance();

  public void setAnalysisTolerance(String tolerance) {
    this.tolerance = tolerance;
  }
}
