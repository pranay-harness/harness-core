package software.wings.sm.states;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Sets;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.scheduler.JobScheduler;
import software.wings.scheduler.LogAnalysisManagerJob;
import software.wings.scheduler.LogClusterManagerJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.LogAnalysisContext;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by rsingh on 7/6/17.
 */
public abstract class AbstractLogAnalysisState extends AbstractAnalysisState {
  public static final String LOG_ML_ROOT = "SPLUNKML_ROOT";
  protected static final String LOG_ML_SHELL_FILE_NAME = "run_splunkml.sh";

  @Inject @Named("VerificationJobScheduler") private QuartzScheduler jobScheduler;

  protected String query;

  public AbstractLogAnalysisState(String name, String stateType) {
    super(name, stateType);
  }

  @Attributes(required = true, title = "Search Keywords", description = "Such as *Exception*")
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

    LogAnalysisContext context = getLogAnalysisContext(executionContext, UUID.randomUUID().toString());

    Set<String> canaryNewHostNames = getCanaryNewHostNames(executionContext);
    if (canaryNewHostNames == null || canaryNewHostNames.isEmpty()) {
      getLogger().error("Could not find test nodes to compare the data");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED, "Could not find test nodes to compare the data");
    }

    Set<String> lastExecutionNodes = getLastExecutionNodes(executionContext);
    if (lastExecutionNodes == null || lastExecutionNodes.isEmpty()) {
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        getLogger().error("No nodes with older version found to compare the logs. Skipping analysis");
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
            "Skipping analysis due to lack of baseline data (First time deployment).");
      }

      getLogger().warn(
          "It seems that there is no successful run for this workflow yet. Log data will be collected to be analyzed for next deployment run");
    }

    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
        && lastExecutionNodes.equals(canaryNewHostNames)) {
      getLogger().error("Control and test nodes are same. Will not be running Log analysis");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED,
          "Skipping analysis due to lack of baseline data (Minimum two phases are required).");
    }

    final LogAnalysisExecutionData executionData =
        LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
            .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
            .withServerConfigID(getAnalysisServerConfigId())
            .withQueries(Sets.newHashSet(query.split(",")))
            .withAnalysisDuration(Integer.parseInt(timeDuration))
            .withStatus(ExecutionStatus.RUNNING)
            .withCanaryNewHostNames(canaryNewHostNames)
            .withLastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : new HashSet<>(lastExecutionNodes))
            .withCorrelationId(context.getCorrelationId())
            .build();

    Set<String> hostsToBeCollected = new HashSet<>();
    if (lastExecutionNodes != null) {
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
      LogAnalysisContext context =
          getLogAnalysisContext(executionContext, executionResponse.getLogAnalysisExecutionData().getCorrelationId());
      final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
          context.getStateExecutionInstanceId(), context.getAppId(), StateType.valueOf(getStateType()));
      if (analysisSummary == null) {
        getLogger().warn("No analysis summary. This can happen if there is no data with the given queries");
        return generateAnalysisResponse(
            context, ExecutionStatus.SUCCESS, "No data found with given queries. Skipped Analysis");
      }

      ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
      if (analysisSummary.getRiskLevel() == RiskLevel.HIGH) {
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
    LogAnalysisContext context = getLogAnalysisContext(executionContext, UUID.randomUUID().toString());

    final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
        context.getStateExecutionInstanceId(), context.getAppId(), StateType.valueOf(getStateType()));

    if (analysisSummary == null) {
      generateAnalysisResponse(context, ExecutionStatus.ABORTED, "Workflow was aborted while analysing");
    }
  }

  private void scheduleAnalysisCronJob(LogAnalysisContext context, String delegateTaskId) {
    Date startDate = new Date(new Date().getTime() + 3 * 60000);
    JobDetail job = JobBuilder.newJob(LogAnalysisManagerJob.class)
                        .withIdentity(context.getStateExecutionInstanceId(), "LOG_VERIFY_CRON_GROUP")
                        .usingJobData("jobParams", context.toJson())
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", delegateTaskId)
                        .withDescription(context.getType() + "-" + context.getStateExecutionInstanceId())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(context.getStateExecutionInstanceId(), "LOG_VERIFY_CRON_GROUP")
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(60).repeatForever())
            .startAt(startDate)
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  private void scheduleClusterCronJob(LogAnalysisContext context, String delegateTaskId) {
    Date startDate = new Date(new Date().getTime() + 3 * 60000);

    JobDetail job = JobBuilder.newJob(LogClusterManagerJob.class)
                        .withIdentity(context.getStateExecutionInstanceId(), "LOG_CLUSTER_CRON_GROUP")
                        .usingJobData("jobParams", context.toJson())
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", delegateTaskId)
                        .withDescription(context.getType() + "-" + context.getStateExecutionInstanceId())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(context.getStateExecutionInstanceId(), "LOG_CLUSTER_CRON_GROUP")
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(60).repeatForever())
            .startAt(startDate)
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  protected ExecutionResponse generateAnalysisResponse(
      LogAnalysisContext context, ExecutionStatus status, String message) {
    analysisService.createAndSaveSummary(context.getType(), context.getAppId(), context.getStateExecutionInstanceId(),
        StringUtils.join(context.getQueries(), ","), message);

    LogAnalysisExecutionData executionData = LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
                                                 .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                                 .withServerConfigID(context.getAnalysisServerConfigId())
                                                 .withQueries(context.getQueries())
                                                 .withAnalysisDuration(Integer.parseInt(context.getTimeDuration()))
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
      default:
        throw new IllegalArgumentException("invalid stateType: " + stateType);
    }
  }

  private LogAnalysisContext getLogAnalysisContext(ExecutionContext context, String correlationId) {
    try {
      return new LogAnalysisContext(context.getAppId(), AbstractLogAnalysisState.this.getWorkflowId(context),
          context.getWorkflowExecutionId(), context.getStateExecutionInstanceId(), getPhaseServiceId(context),
          getLastExecutionNodes(context), getCanaryNewHostNames(context), Sets.newHashSet(query.split(",")),
          AbstractLogAnalysisState.this.configuration.isSslEnabled(),
          AbstractLogAnalysisState.this.configuration.getApplicationPort(),
          AbstractLogAnalysisState.this.appService.get(context.getAppId()).getAccountId(), getComparisonStrategy(),
          timeDuration, getStateType(), getStateBaseUrl(StateType.valueOf(getStateType())), generateAuthToken(),
          getAnalysisServerConfigId(), correlationId);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
