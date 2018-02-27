package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
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
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.metrics.RiskLevel;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.MetricAnalysisJob;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
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

  @Inject @Named("VerificationJobScheduler") private QuartzScheduler jobScheduler;

  @Transient @Inject private MetricDataAnalysisService metricAnalysisService;

  public AbstractMetricAnalysisState(String name, StateType stateType) {
    super(name, stateType.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    getLogger().debug("Executing {} state", getStateType());
    AnalysisContext analysisContext = getAnalysisContext(context, UUID.randomUUID().toString());

    Set<String> canaryNewHostNames = analysisContext.getTestNodes();
    if (isEmpty(canaryNewHostNames)) {
      getLogger().warn("Could not find test nodes to compare the data");
      return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, "Could not find nodes to analyze!");
    }

    Set<String> lastExecutionNodes = analysisContext.getControlNodes();
    if (isEmpty(lastExecutionNodes)) {
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        getLogger().warn("No nodes with older version found to compare the logs. Skipping analysis");
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
            "Skipping analysis due to lack of baseline data (First time deployment).");
      }

      getLogger().warn("It seems that there is no successful run for this workflow yet. "
          + "Metric data will be collected to be analyzed for next deployment run");
    }

    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
        && lastExecutionNodes.equals(canaryNewHostNames)) {
      getLogger().warn("Control and test nodes are same. Will not be running Log analysis");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED,
          "Skipping analysis due to lack of baseline data (Minimum two phases are required).");
    }

    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS) {
      String prevWorkflowExecutionId = metricAnalysisService.getLastSuccessfulWorkflowExecutionIdWithData(
          analysisContext.getStateType(), analysisContext.getWorkflowId(), analysisContext.getServiceId());
      if (prevWorkflowExecutionId == null) {
        getLogger().warn("No previous execution found. This will be the baseline run");
        prevWorkflowExecutionId = "-1";
      }
      analysisContext.setPrevWorkflowExecutionId(prevWorkflowExecutionId);
    }

    final MetricAnalysisExecutionData executionData =
        MetricAnalysisExecutionData.builder()
            .workflowExecutionId(context.getWorkflowExecutionId())
            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
            .serverConfigId(getAnalysisServerConfigId())
            .timeDuration(Integer.parseInt(timeDuration))
            .canaryNewHostNames(canaryNewHostNames)
            .lastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : new HashSet<>(lastExecutionNodes))
            .correlationId(analysisContext.getCorrelationId())
            .build();
    executionData.setStatus(ExecutionStatus.RUNNING);
    Set<String> hostsToCollect = new HashSet<>();
    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS) {
      hostsToCollect.addAll(canaryNewHostNames);
    } else {
      hostsToCollect.addAll(canaryNewHostNames);
      hostsToCollect.addAll(lastExecutionNodes);
    }

    try {
      String delegateTaskId = triggerAnalysisDataCollection(context, executionData.getCorrelationId(), hostsToCollect);

      final MetricDataAnalysisResponse response =
          MetricDataAnalysisResponse.builder().stateExecutionData(executionData).build();
      response.setExecutionStatus(ExecutionStatus.RUNNING);
      scheduleAnalysisCronJob(analysisContext, delegateTaskId);
      saveMetaDataForDashboard(analysisContext.getAccountId(), context);
      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
          .withExecutionStatus(ExecutionStatus.RUNNING)
          .withErrorMessage("New Relic Verification running")
          .withStateExecutionData(executionData)
          .build();
    } catch (Exception ex) {
      getLogger().error("metric analysis state failed", ex);
      return anExecutionResponse()
          .withAsync(true)
          .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withErrorMessage(ex.getMessage())
          .withStateExecutionData(executionData)
          .build();
    }
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
    if (executionResponse.getExecutionStatus() == ExecutionStatus.ERROR
        || executionResponse.getExecutionStatus() == ExecutionStatus.FAILED) {
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.ERROR)
          .withStateExecutionData(executionResponse.getStateExecutionData())
          .withErrorMessage(executionResponse.getStateExecutionData().getErrorMsg())
          .build();
    }

    NewRelicMetricAnalysisRecord metricsAnalysis = metricAnalysisService.getMetricsAnalysis(
        StateType.valueOf(getStateType()), context.getStateExecutionInstanceId(), context.getWorkflowExecutionId());
    if (metricsAnalysis == null) {
      return generateAnalysisResponse(
          context, ExecutionStatus.SUCCESS, "No data found for comparison. Skipping analysis.");
    }
    if (metricsAnalysis.getRiskLevel() == RiskLevel.HIGH) {
      executionStatus = ExecutionStatus.FAILED;
    }

    executionResponse.getStateExecutionData().setStatus(executionStatus);
    getLogger().info("State done with status {}, id: {}", executionStatus, context.getStateExecutionInstanceId());
    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(executionResponse.getStateExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected ExecutionResponse generateAnalysisResponse(
      ExecutionContext context, ExecutionStatus status, String message) {
    final MetricAnalysisExecutionData executionData =
        MetricAnalysisExecutionData.builder()
            .stateExecutionInstanceId(context.getStateExecutionInstanceId())
            .serverConfigId(getAnalysisServerConfigId())
            .timeDuration(Integer.parseInt(timeDuration))
            .correlationId(UUID.randomUUID().toString())
            .build();
    executionData.setStatus(ExecutionStatus.RUNNING);
    NewRelicMetricAnalysisRecord metricAnalysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                            .message(message)
                                                            .stateType(StateType.valueOf(getStateType()))
                                                            .stateExecutionId(context.getStateExecutionInstanceId())
                                                            .workflowExecutionId(context.getWorkflowExecutionId())
                                                            .workflowId(getWorkflowId(context))
                                                            .build();
    metricAnalysisService.saveAnalysisRecords(metricAnalysisRecord);

    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }

  private AnalysisContext getAnalysisContext(ExecutionContext context, String correlationId) {
    try {
      Set<String> controlNodes = getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS
          ? Collections.emptySet()
          : getLastExecutionNodes(context);
      Set<String> testNodes = getCanaryNewHostNames(context);
      controlNodes.removeAll(testNodes);
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
          .timeDuration(Integer.parseInt(timeDuration))
          .stateType(StateType.valueOf(getStateType()))
          .stateBaseUrl(getStateBaseUrl())
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

  public abstract AnalysisTolerance getAnalysisTolerance();

  protected abstract String getStateBaseUrl();
}
