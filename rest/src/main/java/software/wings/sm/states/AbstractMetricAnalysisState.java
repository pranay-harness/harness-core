package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.service.impl.security.SecretManagementDelegateServiceImpl.NUM_OF_RETRIES;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.mongodb.morphia.annotations.Transient;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.api.PcfInstanceElement;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.metrics.RiskLevel;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.MetricAnalysisJob;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 9/25/17.
 */
public abstract class AbstractMetricAnalysisState extends AbstractAnalysisState {
  protected static final int SMOOTH_WINDOW = 3;
  protected static final int TOLERANCE = 1;
  protected static final int MIN_REQUESTS_PER_MINUTE = 10;
  protected static final int COMPARISON_WINDOW = 1;
  protected static final int PARALLEL_PROCESSES = 7;
  public static final int CANARY_DAYS_TO_COLLECT = 7;

  @Inject @Named("VerificationJobScheduler") private QuartzScheduler jobScheduler;

  @Transient @Inject protected MetricDataAnalysisService metricAnalysisService;

  public AbstractMetricAnalysisState(String name, StateType stateType) {
    super(name, stateType.name());
  }

  private void cleanUpForRetry(ExecutionContext executionContext) {
    metricAnalysisService.cleanUpForMetricRetry(executionContext.getStateExecutionInstanceId());
  }

  protected abstract String triggerAnalysisDataCollection(
      ExecutionContext context, String correlationId, Map<String, String> hosts);

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String corelationId = UUID.randomUUID().toString();
    String delegateTaskId = null;
    MetricAnalysisExecutionData executionData;
    try {
      getLogger().info("Executing {} state, id: {} ", getStateType(), context.getStateExecutionInstanceId());
      cleanUpForRetry(context);
      this.analysisContext = getAnalysisContext(context, corelationId);
      getLogger().info("id: {} context: {}", context.getStateExecutionInstanceId(), analysisContext);
      saveMetaDataForDashboard(analysisContext.getAccountId(), context);

      if (isDemoPath(analysisContext.getAccountId()) && getStateType().equals(StateType.NEW_RELIC.name())) {
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
      if (isEmpty(canaryNewHostNames)) {
        getLogger().warn(
            "id: {}, Could not find test nodes to compare the data", context.getStateExecutionInstanceId());
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, "Could not find nodes to analyze!");
      }

      Map<String, String> lastExecutionNodes = analysisContext.getControlNodes();
      if (isEmpty(lastExecutionNodes)) {
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
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS) {
        WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
        String baselineWorkflowExecutionId = workflowExecutionBaselineService.getBaselineExecutionId(context.getAppId(),
            context.getWorkflowId(), workflowStandardParams.getEnv().getUuid(), analysisContext.getServiceId());
        if (isEmpty(baselineWorkflowExecutionId)) {
          responseMessage = "No baseline was set for the workflow. Workflow running with auto baseline.";
          getLogger().info(responseMessage);
          baselineWorkflowExecutionId =
              metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(analysisContext.getStateType(),
                  analysisContext.getAppId(), analysisContext.getWorkflowId(), analysisContext.getServiceId());
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
      int timeDurationInt = Integer.parseInt(timeDuration);
      executionData =
          MetricAnalysisExecutionData.builder()
              .appId(context.getAppId())
              .workflowExecutionId(context.getWorkflowExecutionId())
              .stateExecutionInstanceId(context.getStateExecutionInstanceId())
              .serverConfigId(getAnalysisServerConfigId())
              .timeDuration(timeDurationInt)
              .canaryNewHostNames(canaryNewHostNames.keySet())
              .lastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : lastExecutionNodes.keySet())
              .correlationId(analysisContext.getCorrelationId())
              .canaryNewHostNames(analysisContext.getTestNodes().keySet())
              .lastExecutionNodes(analysisContext.getControlNodes().keySet())
              .build();
      executionData.setErrorMsg(responseMessage);
      executionData.setStatus(ExecutionStatus.RUNNING);
      Map<String, String> hostsToCollect = new HashMap<>();
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS) {
        hostsToCollect.putAll(canaryNewHostNames);
      } else {
        hostsToCollect.putAll(canaryNewHostNames);
        hostsToCollect.putAll(lastExecutionNodes);
      }

      getLogger().info(
          "triggering data collection for {} state, id: {} ", getStateType(), context.getStateExecutionInstanceId());
      hostsToCollect.remove(null);
      createAndSaveMetricGroups(context, hostsToCollect);
      delegateTaskId = triggerAnalysisDataCollection(context, executionData.getCorrelationId(), hostsToCollect);
      getLogger().info("triggered data collection for {} state, id: {}, delgateTaskId: {}", getStateType(),
          context.getStateExecutionInstanceId(), delegateTaskId);

      executionData.setDelegateTaskId(delegateTaskId);
      final MetricDataAnalysisResponse response =
          MetricDataAnalysisResponse.builder().stateExecutionData(executionData).build();
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
      getLogger().error("metric analysis state failed", ex);
      return anExecutionResponse()
          .withAsync(false)
          .withCorrelationIds(Collections.singletonList(corelationId))
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withErrorMessage(Misc.getMessage(ex))
          .withStateExecutionData(MetricAnalysisExecutionData.builder()
                                      .appId(context.getAppId())
                                      .workflowExecutionId(context.getWorkflowExecutionId())
                                      .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                                      .delegateTaskId(delegateTaskId)
                                      .serverConfigId(getAnalysisServerConfigId())
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

  private void scheduleAnalysisCronJob(AnalysisContext context, String delegateTaskId) {
    Date startDate =
        new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(SplunkDataCollectionTask.DELAY_MINUTES + 1));
    JobDetail job =
        JobBuilder.newJob(MetricAnalysisJob.class)
            .withIdentity(context.getStateExecutionId(), getStateType().toUpperCase() + "METRIC_VERIFY_CRON_GROUP")
            .usingJobData("jobParams", JsonUtils.asJson(context))
            .usingJobData("timestamp", System.currentTimeMillis())
            .usingJobData("delegateTaskId", delegateTaskId)
            .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
            .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(context.getStateExecutionId(), getStateType().toUpperCase() + "METRIC_VERIFY_CRON_GROUP")
            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                              .withIntervalInSeconds(60)
                              .withMisfireHandlingInstructionNowWithExistingCount()
                              .repeatForever())
            .startAt(startDate)
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    MetricDataAnalysisResponse executionResponse = (MetricDataAnalysisResponse) response.values().iterator().next();

    if (ExecutionStatus.isBrokeStatus(executionResponse.getExecutionStatus())) {
      getLogger().info(
          "for {} got failed execution response {}", context.getStateExecutionInstanceId(), executionResponse);
      continuousVerificationService.setMetaDataExecutionStatus(
          context.getStateExecutionInstanceId(), ExecutionStatus.FAILED);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withStateExecutionData(executionResponse.getStateExecutionData())
          .withErrorMessage(executionResponse.getStateExecutionData().getErrorMsg())
          .build();
    }

    int analysisMinute = executionResponse.getStateExecutionData().getAnalysisMinute();
    for (int i = 0; i < NUM_OF_RETRIES; i++) {
      List<NewRelicMetricAnalysisRecord> metricAnalysisRecords = metricAnalysisService.getMetricsAnalysis(
          context.getAppId(), context.getStateExecutionInstanceId(), context.getWorkflowExecutionId());
      if (isEmpty(metricAnalysisRecords)) {
        getLogger().info("for {} No analysis summary.", context.getStateExecutionInstanceId());
        continuousVerificationService.setMetaDataExecutionStatus(
            context.getStateExecutionInstanceId(), ExecutionStatus.SUCCESS);
        return generateAnalysisResponse(
            context, ExecutionStatus.SUCCESS, "No data found for comparison. Please check load. Skipping analysis.");
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
      getLogger().info("for {} found analysisSummary with analysis records {}", context.getStateExecutionInstanceId(),
          metricAnalysisRecords.size());
      for (NewRelicMetricAnalysisRecord metricAnalysisRecord : metricAnalysisRecords) {
        if (metricAnalysisRecord.getRiskLevel() == RiskLevel.HIGH) {
          executionStatus = ExecutionStatus.FAILED;
        }
      }
      executionResponse.getStateExecutionData().setStatus(executionStatus);
      getLogger().info("State done with status {}, id: {}", executionStatus, context.getStateExecutionInstanceId());
      continuousVerificationService.setMetaDataExecutionStatus(context.getStateExecutionInstanceId(), executionStatus);
      return anExecutionResponse()
          .withExecutionStatus(executionStatus)
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
        context.getStateExecutionInstanceId(), ExecutionStatus.ABORTED);
  }

  protected ExecutionResponse generateAnalysisResponse(
      ExecutionContext context, ExecutionStatus status, String message) {
    final MetricAnalysisExecutionData executionData =
        MetricAnalysisExecutionData.builder()
            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
            .serverConfigId(getAnalysisServerConfigId())
            .timeDuration(Integer.parseInt(timeDuration))
            .correlationId(UUID.randomUUID().toString())
            .build();
    executionData.setStatus(status);
    NewRelicMetricAnalysisRecord metricAnalysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                            .message(message)
                                                            .appId(context.getAppId())
                                                            .stateType(StateType.valueOf(getStateType()))
                                                            .stateExecutionId(context.getStateExecutionInstanceId())
                                                            .workflowExecutionId(context.getWorkflowExecutionId())
                                                            .workflowId(getWorkflowId(context))
                                                            .build();
    metricAnalysisService.saveAnalysisRecords(metricAnalysisRecord);
    continuousVerificationService.setMetaDataExecutionStatus(context.getStateExecutionInstanceId(), status);

    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }

  private AnalysisContext getAnalysisContext(ExecutionContext context, String correlationId) {
    try {
      Map<String, String> controlNodes = getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS
          ? Collections.emptyMap()
          : getLastExecutionNodes(context);
      Map<String, String> testNodes = getCanaryNewHostNames(context);
      testNodes.keySet().forEach(testNode -> controlNodes.remove(testNode));
      int timeDurationInt = Integer.parseInt(timeDuration);
      return AnalysisContext.builder()
          .accountId(this.appService.get(context.getAppId()).getAccountId())
          .appId(context.getAppId())
          .workflowId(getWorkflowId(context))
          .workflowExecutionId(context.getWorkflowExecutionId())
          .stateExecutionId(context.getStateExecutionInstanceId())
          .serviceId(getPhaseServiceId(context))
          .controlNodes(controlNodes)
          .testNodes(testNodes)
          .isSSL(this.configuration.isSslEnabled())
          .appPort(this.configuration.getApplicationPort())
          .comparisonStrategy(getComparisonStrategy())
          .timeDuration(timeDurationInt)
          .stateType(StateType.valueOf(getStateType()))
          .authToken(generateAuthToken())
          .analysisServerConfigId(getAnalysisServerConfigId())
          .correlationId(correlationId)
          .smooth_window(SMOOTH_WINDOW)
          .tolerance(getAnalysisTolerance().tolerance())
          .minimumRequestsPerMinute(MIN_REQUESTS_PER_MINUTE)
          .comparisonWindow(COMPARISON_WINDOW)
          .parallelProcesses(PARALLEL_PROCESSES)
          .build();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
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
