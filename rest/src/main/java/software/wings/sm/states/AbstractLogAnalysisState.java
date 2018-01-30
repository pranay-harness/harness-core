package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.metrics.RiskLevel;
import software.wings.scheduler.LogAnalysisManagerJob;
import software.wings.scheduler.LogClusterManagerJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
  public static final String LOG_ML_ROOT = "SPLUNKML_ROOT";
  protected static final String LOG_ML_SHELL_FILE_NAME = "run_splunkml.sh";
  protected static final int HOST_BATCH_SIZE = 5;
  @Inject @Named("VerificationJobScheduler") private QuartzScheduler jobScheduler;

  protected String query;

  @Transient @Inject @SchemaIgnore protected AnalysisService analysisService;

  public AbstractLogAnalysisState(String name, String stateType) {
    super(name, stateType);
  }

  @Attributes(required = true, title = "Search Keywords")
  @DefaultValue("*exception*")
  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext executionContext) {
    getLogger().debug("Executing analysis state");

    AnalysisContext context = getLogAnalysisContext(executionContext, UUID.randomUUID().toString());

    Set<String> canaryNewHostNames = context.getTestNodes();
    if (isEmpty(canaryNewHostNames)) {
      getLogger().error("Could not find test nodes to compare the data");
      return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, "Could not find hosts to analyze!");
    }

    Set<String> lastExecutionNodes = context.getControlNodes();
    if (isEmpty(lastExecutionNodes)) {
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        getLogger().error("No nodes with older version found to compare the logs. Skipping analysis");
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
            "Skipping analysis due to lack of baseline hosts. Make sure you have at least two phases defined.");
      }

      getLogger().warn("It seems that there is no successful run for this workflow yet. "
          + "Log data will be collected to be analyzed for next deployment run");
    }

    //    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
    //        && lastExecutionNodes.equals(canaryNewHostNames)) {
    //      getLogger().error("Control and test nodes are same. Will not be running Log analysis");
    //      return generateAnalysisResponse(context, ExecutionStatus.FAILED,
    //          "Skipping analysis. Baseline and new hosts are the same. (Minimum two phases are required).");
    //    }

    final LogAnalysisExecutionData executionData =
        LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
            .withStateExecutionInstanceId(context.getStateExecutionId())
            .withServerConfigID(getAnalysisServerConfigId())
            .withQueries(Sets.newHashSet(query.split(",")))
            .withAnalysisDuration(Integer.parseInt(timeDuration))
            .withStatus(ExecutionStatus.RUNNING)
            .withCanaryNewHostNames(canaryNewHostNames)
            .withLastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : new HashSet<>(lastExecutionNodes))
            .withCorrelationId(context.getCorrelationId())
            .build();

    Set<String> hostsToBeCollected = new HashSet<>();
    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT && lastExecutionNodes != null) {
      hostsToBeCollected.addAll(lastExecutionNodes);
    }
    hostsToBeCollected.addAll(canaryNewHostNames);
    String delegateTaskId =
        triggerAnalysisDataCollection(executionContext, context.getCorrelationId(), hostsToBeCollected);

    scheduleClusterCronJob(context, delegateTaskId);
    scheduleAnalysisCronJob(context, delegateTaskId);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(context.getCorrelationId()))
        .withExecutionStatus(ExecutionStatus.RUNNING)
        .withErrorMessage("Log Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(
      ExecutionContext executionContext, Map<String, NotifyResponseData> response) {
    LogAnalysisResponse executionResponse = (LogAnalysisResponse) response.values().iterator().next();
    if (executionResponse.getExecutionStatus() == ExecutionStatus.FAILED) {
      return anExecutionResponse()
          .withExecutionStatus(executionResponse.getExecutionStatus())
          .withStateExecutionData(executionResponse.getLogAnalysisExecutionData())
          .withErrorMessage(executionResponse.getLogAnalysisExecutionData().getErrorMsg())
          .build();
    } else {
      AnalysisContext context =
          getLogAnalysisContext(executionContext, executionResponse.getLogAnalysisExecutionData().getCorrelationId());
      final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
          context.getStateExecutionId(), context.getAppId(), StateType.valueOf(getStateType()));
      if (analysisSummary == null) {
        getLogger().warn("No analysis summary. This can happen if there is no data with the given queries");
        return generateAnalysisResponse(
            context, ExecutionStatus.SUCCESS, "No data found with given queries. Skipped Analysis");
      }

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

      executionResponse.getLogAnalysisExecutionData().setStatus(executionStatus);
      return anExecutionResponse()
          .withExecutionStatus(executionStatus)
          .withStateExecutionData(executionResponse.getLogAnalysisExecutionData())
          .build();
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext executionContext) {
    AnalysisContext context = getLogAnalysisContext(executionContext, UUID.randomUUID().toString());

    final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
        context.getStateExecutionId(), context.getAppId(), StateType.valueOf(getStateType()));

    if (analysisSummary == null) {
      generateAnalysisResponse(context, ExecutionStatus.ABORTED, "Workflow was aborted while analysing");
    }
  }

  private void scheduleAnalysisCronJob(AnalysisContext context, String delegateTaskId) {
    Date startDate =
        new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(SplunkDataCollectionTask.DELAY_MINUTES + 1));
    JobDetail job = JobBuilder.newJob(LogAnalysisManagerJob.class)
                        .withIdentity(context.getStateExecutionId(), "LOG_VERIFY_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", delegateTaskId)
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(context.getStateExecutionId(), "LOG_VERIFY_CRON_GROUP")
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(10)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  private void scheduleClusterCronJob(AnalysisContext context, String delegateTaskId) {
    Date startDate = new Date(new Date().getTime() + 3 * 60000);

    JobDetail job = JobBuilder.newJob(LogClusterManagerJob.class)
                        .withIdentity(context.getStateExecutionId(), "LOG_CLUSTER_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", delegateTaskId)
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(context.getStateExecutionId(), "LOG_CLUSTER_CRON_GROUP")
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(10)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  protected ExecutionResponse generateAnalysisResponse(
      AnalysisContext context, ExecutionStatus status, String message) {
    analysisService.createAndSaveSummary(context.getStateType(), context.getAppId(), context.getStateExecutionId(),
        StringUtils.join(context.getQueries(), ","), message);

    LogAnalysisExecutionData executionData = LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
                                                 .withStateExecutionInstanceId(context.getStateExecutionId())
                                                 .withServerConfigID(context.getAnalysisServerConfigId())
                                                 .withQueries(context.getQueries())
                                                 .withAnalysisDuration(context.getTimeDuration())
                                                 .withStatus(status)
                                                 .withCorrelationId(context.getCorrelationId())
                                                 .build();

    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
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

  private AnalysisContext getLogAnalysisContext(ExecutionContext context, String correlationId) {
    try {
      Set<String> controlNodes = getLastExecutionNodes(context);
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
          .queries(Sets.newHashSet(query.split(",")))
          .isSSL(this.configuration.isSslEnabled())
          .appPort(this.configuration.getApplicationPort())
          .comparisonStrategy(getComparisonStrategy())
          .timeDuration(Integer.parseInt(timeDuration))
          .stateType(StateType.valueOf(getStateType()))
          .stateBaseUrl(getStateBaseUrl(StateType.valueOf(getStateType())))
          .authToken(generateAuthToken())
          .analysisServerConfigId(getAnalysisServerConfigId())
          .correlationId(correlationId)
          .build();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
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
