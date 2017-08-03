package software.wings.sm.states;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Sets;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.AnalysisComparisonStrategy;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 7/6/17.
 */
public abstract class AbstractLogAnalysisState extends AbstractAnalysisState {
  protected String query;

  @Transient @SchemaIgnore protected ScheduledExecutorService pythonExecutorService;

  @Attributes(required = true, title = "Query")
  @DefaultValue("*exception*")
  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public AbstractLogAnalysisState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    getLogger().debug("Executing analysis state");

    Set<String> canaryNewHostNames = getCanaryNewHostNames(context);
    if (canaryNewHostNames == null || canaryNewHostNames.isEmpty()) {
      getLogger().error("Could not find test nodes to compare the data");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED, getAnalysisServerConfigId(),
          "Could not find test nodes to compare the data");
    }

    Set<String> lastExecutionNodes = getLastExecutionNodes(context);
    if (lastExecutionNodes == null || lastExecutionNodes.isEmpty()) {
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        getLogger().error("No nodes with older version found to compare the logs. Skipping analysis");
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, getAnalysisServerConfigId(),
            "Skipping analysis due to lack of baseline data (First time deployment).");
      }

      triggerAnalysisDataCollection(context, canaryNewHostNames);
      getLogger().warn(
          "It seems that there is no successful run for this workflow yet. Log data will be collected to be analyzed for next deployment run");
      return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, getAnalysisServerConfigId(),
          "Skipping analysis due to lack of baseline data (First time deployment).");
    }

    final LogAnalysisExecutionData executionData =
        LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
            .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
            .withServerConfigID(getAnalysisServerConfigId())
            .withQueries(Sets.newHashSet(query.split(",")))
            .withAnalysisDuration(Integer.parseInt(timeDuration))
            .withStatus(ExecutionStatus.RUNNING)
            .withCanaryNewHostNames(canaryNewHostNames)
            .withLastExecutionNodes(new HashSet<>(lastExecutionNodes))
            .withCorrelationId(UUID.randomUUID().toString())
            .build();

    lastExecutionNodes.removeAll(canaryNewHostNames);
    if (lastExecutionNodes.isEmpty()) {
      getLogger().error("Control and test nodes are same. Will not be running splunk analysis");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED, getAnalysisServerConfigId(),
          "Skipping analysis due to lack of baseline data (Minimum two phases are required).");
    }

    Set<String> hostsToBeCollected = new HashSet<>();
    hostsToBeCollected.addAll(lastExecutionNodes);
    hostsToBeCollected.addAll(canaryNewHostNames);
    triggerAnalysisDataCollection(context, hostsToBeCollected);

    final LogAnalysisResponse response = aLogAnalysisResponse()
                                             .withLogAnalysisExecutionData(executionData)
                                             .withExecutionStatus(ExecutionStatus.SUCCESS)
                                             .build();
    pythonExecutorService = createPythonExecutorService(context);
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.schedule(() -> {
      try {
        pythonExecutorService.shutdown();
        pythonExecutorService.awaitTermination(1, TimeUnit.MINUTES);
        waitNotifyEngine.notify(executionData.getCorrelationId(), response);
      } catch (InterruptedException e) {
        pythonExecutorService.shutdown();
      }
    }, Long.parseLong(timeDuration) + SplunkDataCollectionTask.DELAY_MINUTES + 1, TimeUnit.MINUTES);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
        .withExecutionStatus(ExecutionStatus.RUNNING)
        .withErrorMessage("Log Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
        context.getStateExecutionInstanceId(), context.getAppId(), StateType.SPLUNKV2);
    if (analysisSummary == null) {
      getLogger().warn("No analysis summary. This can happen if there is no data with the given queries");
      return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, getAnalysisServerConfigId(),
          "No data found with given queries. Skipped Analysis");
    }

    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    if (analysisSummary.getRiskLevel() == RiskLevel.HIGH) {
      getLogger().error("Found anomolies. Marking it failed." + analysisSummary.getAnalysisSummaryMessage());
      executionStatus = ExecutionStatus.FAILED;
    }

    LogAnalysisResponse executionResponse = (LogAnalysisResponse) response.values().iterator().next();
    executionResponse.getLogAnalysisExecutionData().setStatus(executionStatus);
    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(executionResponse.getLogAnalysisExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    try {
      pythonExecutorService.shutdown();
      pythonExecutorService.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      pythonExecutorService.shutdown();
    }

    final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
        context.getStateExecutionInstanceId(), context.getAppId(), StateType.SPLUNKV2);

    if (analysisSummary == null) {
      generateAnalysisResponse(
          context, ExecutionStatus.ABORTED, getAnalysisServerConfigId(), "Workflow was aborted while analysing");
    }
  }

  private ScheduledExecutorService createPythonExecutorService(ExecutionContext context) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        getLogAnanlysisGenerator(context), SplunkDataCollectionTask.DELAY_MINUTES + 1, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  @SchemaIgnore protected abstract Runnable getLogAnanlysisGenerator(ExecutionContext context);

  protected ExecutionResponse generateAnalysisResponse(
      ExecutionContext context, ExecutionStatus status, String serverConfigId, String message) {
    LogAnalysisExecutionData executionData = LogAnalysisExecutionData.Builder.anLogAnanlysisExecutionData()
                                                 .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                                 .withServerConfigID(serverConfigId)
                                                 .withQueries(Sets.newHashSet(query.split(",")))
                                                 .withAnalysisDuration(Integer.parseInt(timeDuration))
                                                 .withStatus(status)
                                                 .withCorrelationId(UUID.randomUUID().toString())
                                                 .build();
    for (String splunkQuery : query.split(",")) {
      final LogMLAnalysisRecord analysisRecord = new LogMLAnalysisRecord();
      analysisRecord.setStateType(StateType.valueOf(getStateType()));
      analysisRecord.setApplicationId(context.getAppId());
      analysisRecord.setStateExecutionId(context.getStateExecutionInstanceId());
      executionData.setStatus(status);
      analysisRecord.setQuery(splunkQuery);
      analysisRecord.setAnalysisSummaryMessage(message);
      analysisRecord.setControl_events(Collections.emptyMap());
      analysisRecord.setTest_events(Collections.emptyMap());
      analysisService.saveLogAnalysisRecords(analysisRecord, StateType.SPLUNKV2);
    }

    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }
}
