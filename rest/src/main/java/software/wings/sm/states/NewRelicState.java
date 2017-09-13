package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.api.PhaseElement;
import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicExecutionData;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisJob;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicSettingProvider;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicState extends AbstractAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(NewRelicState.class);

  @Inject @Named("VerificationJobScheduler") private QuartzScheduler jobScheduler;

  @Transient @SchemaIgnore private ScheduledExecutorService analysisExecutorService;

  @Transient @Inject private NewRelicService newRelicService;

  @EnumData(enumDataProvider = NewRelicSettingProvider.class)
  @Attributes(required = true, title = "New Relic Server")
  private String analysisServerConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationId;

  public NewRelicState(String name) {
    super(name, StateType.NEW_RELIC.name());
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (StringUtils.isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (StringUtils.isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, String correlationId, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No new relic setting with id: " + analysisServerConfigId + " found");
    }

    final NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute.getValue();

    final long dataCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final NewRelicDataCollectionInfo dataCollectionInfo = NewRelicDataCollectionInfo.builder()
                                                              .newRelicConfig(newRelicConfig)
                                                              .applicationId(context.getAppId())
                                                              .stateExecutionId(context.getStateExecutionInstanceId())
                                                              .workflowId(getWorkflowId(context))
                                                              .workflowExecutionId(context.getWorkflowExecutionId())
                                                              .serviceId(getPhaseServiceId(context))
                                                              .startTime(dataCollectionStartTimeStamp)
                                                              .collectionTime(Integer.parseInt(timeDuration))
                                                              .newRelicAppId(Long.parseLong(applicationId))
                                                              .dataCollectionMinute(0)
                                                              .build();

    String waitId = UUIDGenerator.getUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.NEWRELIC_COLLECT_METRIC_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId), waitId);
    return delegateService.queueTask(delegateTask);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    logger.debug("Executing new relic state");
    AnalysisContext analysisContext = getAnalysisContext(context, UUID.randomUUID().toString());

    Set<String> canaryNewHostNames = analysisContext.getTestNodes();
    if (canaryNewHostNames == null || canaryNewHostNames.isEmpty()) {
      getLogger().error("Could not find test nodes to compare the data");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED, "Could not find test nodes to compare the data");
    }

    Set<String> lastExecutionNodes = analysisContext.getControlNodes();
    if (lastExecutionNodes == null || lastExecutionNodes.isEmpty()) {
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        getLogger().error("No nodes with older version found to compare the logs. Skipping analysis");
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
            "Skipping analysis due to lack of baseline data (First time deployment).");
      }

      getLogger().warn(
          "It seems that there is no successful run for this workflow yet. Metric data will be collected to be analyzed for next deployment run");
    }

    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
        && lastExecutionNodes.equals(canaryNewHostNames)) {
      getLogger().error("Control and test nodes are same. Will not be running Log analysis");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED,
          "Skipping analysis due to lack of baseline data (Minimum two phases are required).");
    }

    final NewRelicExecutionData executionData =
        NewRelicExecutionData.Builder.anAnanlysisExecutionData()
            .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
            .withServerConfigID(getAnalysisServerConfigId())
            .withAnalysisDuration(Integer.parseInt(timeDuration))
            .withStatus(ExecutionStatus.RUNNING)
            .withCanaryNewHostNames(canaryNewHostNames)
            .withLastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : new HashSet<>(lastExecutionNodes))
            .withCorrelationId(analysisContext.getCorrelationId())
            .build();
    String delegateTaskId = triggerAnalysisDataCollection(context, executionData.getCorrelationId(), null);

    final MetricDataAnalysisResponse response =
        MetricDataAnalysisResponse.builder().stateExecutionData(executionData).build();
    response.setExecutionStatus(ExecutionStatus.RUNNING);
    scheduleAnalysisCronJob(analysisContext, delegateTaskId);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
        .withExecutionStatus(ExecutionStatus.RUNNING)
        .withErrorMessage("New Relic Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  private void scheduleAnalysisCronJob(AnalysisContext context, String delegateTaskId) {
    Date startDate =
        new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(SplunkDataCollectionTask.DELAY_MINUTES + 1));
    JobDetail job = JobBuilder.newJob(NewRelicMetricAnalysisJob.class)
                        .withIdentity(context.getStateExecutionId(), "NEWRELIC_METRIC_VERIFY_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", delegateTaskId)
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(context.getStateExecutionId(), "NEWRELIC_METRIC_VERIFY_CRON_GROUP")
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(60).repeatForever())
            .startAt(startDate)
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    MetricDataAnalysisResponse executionResponse = (MetricDataAnalysisResponse) response.values().iterator().next();
    if (executionResponse.getExecutionStatus() == ExecutionStatus.FAILED) {
      return anExecutionResponse()
          .withExecutionStatus(executionResponse.getExecutionStatus())
          .withStateExecutionData(executionResponse.getStateExecutionData())
          .withErrorMessage(executionResponse.getStateExecutionData().getErrorMsg())
          .build();
    }

    NewRelicMetricAnalysisRecord metricsAnalysis =
        newRelicService.getMetricsAnalysis(context.getStateExecutionInstanceId(), context.getWorkflowExecutionId());
    if (metricsAnalysis == null) {
      return generateAnalysisResponse(
          context, ExecutionStatus.SUCCESS, "No data found for comparison. Skipping analysis.");
    }
    if (metricsAnalysis.getRiskLevel() == RiskLevel.HIGH) {
      executionStatus = ExecutionStatus.FAILED;
    }

    executionResponse.getStateExecutionData().setStatus(executionStatus);
    logger.info("State done with status {}", executionStatus);
    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(executionResponse.getStateExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected ExecutionResponse generateAnalysisResponse(
      ExecutionContext context, ExecutionStatus status, String message) {
    final NewRelicExecutionData executionData = NewRelicExecutionData.Builder.anAnanlysisExecutionData()
                                                    .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                                    .withServerConfigID(getAnalysisServerConfigId())
                                                    .withAnalysisDuration(Integer.parseInt(timeDuration))
                                                    .withStatus(ExecutionStatus.RUNNING)
                                                    .withCorrelationId(UUID.randomUUID().toString())
                                                    .build();
    NewRelicMetricAnalysisRecord metricAnalysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                            .message(message)
                                                            .stateExecutionId(context.getStateExecutionInstanceId())
                                                            .workflowExecutionId(context.getWorkflowExecutionId())
                                                            .workflowId(getWorkflowId(context))
                                                            .build();
    newRelicService.saveAnalysisRecords(metricAnalysisRecord);

    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }

  private AnalysisContext getAnalysisContext(ExecutionContext context, String correlationId) {
    try {
      return AnalysisContext.builder()
          .accountId(this.appService.get(context.getAppId()).getAccountId())
          .appId(context.getAppId())
          .workflowId(getWorkflowId(context))
          .workflowExecutionId(context.getWorkflowExecutionId())
          .stateExecutionId(context.getStateExecutionInstanceId())
          .serviceId(getPhaseServiceId(context))
          .controlNodes(getLastExecutionNodes(context))
          .testNodes(getCanaryNewHostNames(context))
          .isSSL(this.configuration.isSslEnabled())
          .appPort(this.configuration.getApplicationPort())
          .comparisonStrategy(getComparisonStrategy())
          .timeDuration(Integer.parseInt(timeDuration))
          .stateType(StateType.valueOf(getStateType()))
          .stateBaseUrl(getStateBaseUrl())
          .authToken(generateAuthToken())
          .analysisServerConfigId(getAnalysisServerConfigId())
          .correlationId(correlationId)
          .build();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @SchemaIgnore
  public static String getStateBaseUrl() {
    return "newrelic";
  }
}
