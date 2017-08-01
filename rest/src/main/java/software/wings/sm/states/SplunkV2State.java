package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.service.impl.splunk.SplunkAnalysisResponse.Builder.anSplunkAnalysisResponse;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.AnalysisComparisonStrategy;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.TaskType;
import software.wings.common.UUIDGenerator;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.exception.WingsException;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.splunk.SplunkAnalysisResponse;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.impl.splunk.SplunkExecutionData;
import software.wings.service.impl.splunk.SplunkLogCollectionCallback;
import software.wings.service.impl.splunk.SplunkLogMLAnalysisRecord;
import software.wings.service.impl.splunk.SplunkMLAnalysisSummary;
import software.wings.service.impl.splunk.SplunkSettingProvider;
import software.wings.service.intfc.splunk.SplunkService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
public class SplunkV2State extends AbstractAnalysisState {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(SplunkV2State.class);

  private static final String SPLUNKML_ROOT = "SPLUNKML_ROOT";
  private static final String SPLUNKML_SHELL_FILE_NAME = "run_splunkml.sh";

  @EnumData(enumDataProvider = SplunkSettingProvider.class)
  @Attributes(required = true, title = "Splunk Server")
  private String splunkConfigId;

  @Attributes(required = true, title = "Query") private String query;

  @DefaultValue("15")
  @Attributes(title = "Analyze Time duration (in minutes)", description = "Default 15 minutes")
  private String timeDuration;

  @Transient @Inject private SplunkService splunkService;

  @Transient @SchemaIgnore private ScheduledExecutorService pythonExecutorService;

  public SplunkV2State(String name) {
    super(name, StateType.SPLUNKV2.getType());
  }

  /**
   * Getter for property 'query'.
   *
   * @return Value for property 'query'.
   */
  public String getQuery() {
    return query;
  }

  /**
   * Setter for property 'query'.
   *
   * @param query Value to set for property 'query'.
   */
  public void setQuery(String query) {
    this.query = query;
  }

  public String getTimeDuration() {
    return timeDuration;
  }

  public void setTimeDuration(String timeDuration) {
    this.timeDuration = timeDuration;
  }

  public String getSplunkConfigId() {
    return splunkConfigId;
  }

  public void setSplunkConfigId(String splunkConfigId) {
    this.splunkConfigId = splunkConfigId;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    logger.debug("Executing splunk state");

    Set<String> canaryNewHostNames = getCanaryNewHostNames(context);
    if (canaryNewHostNames == null || canaryNewHostNames.isEmpty()) {
      logger.error("Could not find test nodes to compare the data");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED, "Could not find test nodes to compare the data");
    }

    Set<String> lastExecutionNodes = getLastExecutionNodes(context);
    if (lastExecutionNodes == null || lastExecutionNodes.isEmpty()) {
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        logger.error("No nodes with older version found to compare the logs. Skipping analysis");
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
            "Skipping analysis due to lack of baseline data (First time deployment).");
      }

      triggerSplunkDataCollection(context);
      logger.warn(
          "It seems that there is no successful run for this workflow yet. Log data will be collected to be analyzed for next deployment run");
      return generateAnalysisResponse(
          context, ExecutionStatus.SUCCESS, "Skipping analysis due to lack of baseline data (First time deployment).");
    }

    final SplunkExecutionData executionData = SplunkExecutionData.Builder.anSplunkExecutionData()
                                                  .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                                  .withSplunkConfigID(splunkConfigId)
                                                  .withSplunkQueries(Sets.newHashSet(query.split(",")))
                                                  .withAnalysisDuration(Integer.parseInt(timeDuration))
                                                  .withStatus(ExecutionStatus.RUNNING)
                                                  .withCanaryNewHostNames(canaryNewHostNames)
                                                  .withLastExecutionNodes(new HashSet<>(lastExecutionNodes))
                                                  .withCorrelationId(UUID.randomUUID().toString())
                                                  .build();

    lastExecutionNodes.removeAll(canaryNewHostNames);
    if (lastExecutionNodes.isEmpty()) {
      logger.error("Control and test nodes are same. Will not be running splunk analysis");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED,
          "Skipping analysis due to lack of baseline data (Minimum two phases are required).");
    }

    triggerSplunkDataCollection(context);

    final SplunkAnalysisResponse response = anSplunkAnalysisResponse()
                                                .withSplunkExecutionData(executionData)
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
        .withErrorMessage("Splunk Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    final SplunkMLAnalysisSummary analysisSummary =
        splunkService.getAnalysisSummary(context.getStateExecutionInstanceId(), context.getAppId());
    if (analysisSummary == null) {
      logger.warn("No analysis summary. This can happen if there is no data with the given queries");
      return generateAnalysisResponse(
          context, ExecutionStatus.SUCCESS, "No data found with given queries. Skipped Analysis");
    }

    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    if (analysisSummary.getRiskLevel() == RiskLevel.HIGH) {
      logger.error("Found anomolies. Marking it failed." + analysisSummary.getAnalysisSummaryMessage());
      executionStatus = ExecutionStatus.FAILED;
    }

    SplunkAnalysisResponse executionResponse = (SplunkAnalysisResponse) response.values().iterator().next();
    executionResponse.getSplunkExecutionData().setStatus(executionStatus);
    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(executionResponse.getSplunkExecutionData())
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

    final SplunkMLAnalysisSummary analysisSummary =
        splunkService.getAnalysisSummary(context.getStateExecutionInstanceId(), context.getAppId());

    if (analysisSummary == null) {
      generateAnalysisResponse(context, ExecutionStatus.ABORTED, "Workflow was aborted while analysing");
    }
  }

  private ExecutionResponse generateAnalysisResponse(ExecutionContext context, ExecutionStatus status, String message) {
    SplunkExecutionData executionData = SplunkExecutionData.Builder.anSplunkExecutionData()
                                            .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                            .withSplunkConfigID(splunkConfigId)
                                            .withSplunkQueries(Sets.newHashSet(query.split(",")))
                                            .withAnalysisDuration(Integer.parseInt(timeDuration))
                                            .withStatus(status)
                                            .withCorrelationId(UUID.randomUUID().toString())
                                            .build();
    for (String splunkQuery : query.split(",")) {
      final SplunkLogMLAnalysisRecord analysisRecord = new SplunkLogMLAnalysisRecord();
      analysisRecord.setApplicationId(context.getAppId());
      analysisRecord.setStateExecutionId(context.getStateExecutionInstanceId());
      executionData.setStatus(status);
      analysisRecord.setQuery(splunkQuery);
      analysisRecord.setAnalysisSummaryMessage(message);
      analysisRecord.setControl_events(Collections.emptyMap());
      analysisRecord.setTest_events(Collections.emptyMap());
      splunkService.saveSplunkAnalysisRecords(analysisRecord);
    }

    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }

  private ScheduledExecutorService createPythonExecutorService(ExecutionContext context) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new SplunkAnalysisGenerator(context), SplunkDataCollectionTask.DELAY_MINUTES + 1, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private long triggerSplunkDataCollection(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(splunkConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No splunk setting with id: " + splunkConfigId + " found");
    }

    final SplunkConfig splunkConfig = (SplunkConfig) settingAttribute.getValue();
    final Set<String> queries = Sets.newHashSet(query.split(","));
    final long logCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final SplunkDataCollectionInfo dataCollectionInfo = new SplunkDataCollectionInfo(
        appService.get(context.getAppId()).getAccountId(), context.getAppId(), context.getStateExecutionInstanceId(),
        getWorkflowId(context), splunkConfig, queries, logCollectionStartTimeStamp, Integer.parseInt(timeDuration));
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.SPLUNK_COLLECT_LOG_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .build();
    waitNotifyEngine.waitForAll(new SplunkLogCollectionCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
    return logCollectionStartTimeStamp;
  }

  private class SplunkAnalysisGenerator implements Runnable {
    private final ExecutionContext context;
    private final String pythonScriptRoot;
    private final String serverUrl;
    private final String accountId;
    private final String applicationId;
    private final String workflowId;
    private final Set<String> testNodes;
    private final Set<String> controlNodes;
    private final Set<String> queries;
    private int logCollectionMinute = 0;

    public SplunkAnalysisGenerator(ExecutionContext context) {
      this.context = context;
      this.pythonScriptRoot = System.getenv(SPLUNKML_ROOT);
      Preconditions.checkState(!StringUtils.isBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");

      String protocol = SplunkV2State.this.configuration.isSslEnabled() ? "https" : "http";
      this.serverUrl = protocol + "://localhost:" + SplunkV2State.this.configuration.getApplicationPort();
      this.applicationId = context.getAppId();
      this.accountId = SplunkV2State.this.appService.get(this.applicationId).getAccountId();
      this.workflowId = getWorkflowId(context);
      this.testNodes = getCanaryNewHostNames(context);
      this.controlNodes = getLastExecutionNodes(context);
      this.controlNodes.removeAll(this.testNodes);
      this.queries = Sets.newHashSet(query.split(","));
    }

    @Override
    public void run() {
      if (logCollectionMinute > Integer.parseInt(timeDuration)) {
        return;
      }

      for (String query : queries) {
        if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            && !splunkService.isLogDataCollected(
                   applicationId, context.getStateExecutionInstanceId(), query, logCollectionMinute)) {
          logger.warn("No data collected for minute " + logCollectionMinute + " for application: " + applicationId
              + " stateExecution: " + context.getStateExecutionInstanceId()
              + ". No ML analysis will be run this minute");
          continue;
        }

        try {
          final long endTime = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis()) - 1;
          final String testInputUrl =
              this.serverUrl + "/api/splunk/get-logs?accountId=" + accountId + "&compareCurrent=true";
          String controlInputUrl = this.serverUrl + "/api/splunk/get-logs?accountId=" + accountId + "&compareCurrent=";
          controlInputUrl = getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
              ? controlInputUrl + true
              : controlInputUrl + false;
          final String logAnalysisSaveUrl = this.serverUrl + "/api/splunk/save-analysis-records?accountId=" + accountId
              + "&applicationId=" + applicationId + "&stateExecutionId=" + context.getStateExecutionInstanceId();
          final String logAnalysisGetUrl = this.serverUrl + "/api/splunk/get-analysis-records?accountId=" + accountId;
          final List<String> command = new ArrayList<>();
          command.add(this.pythonScriptRoot + "/" + SPLUNKML_SHELL_FILE_NAME);
          command.add("--query=" + query);
          command.add("--control_input_url");
          command.add(controlInputUrl);
          command.add("--test_input_url");
          command.add(testInputUrl);
          command.add("--auth_token=" + generateAuthToken());
          command.add("--application_id=" + applicationId);
          command.add("--workflow_id=" + workflowId);
          command.add("--control_nodes");
          command.addAll(controlNodes);
          command.add("--test_nodes");
          command.addAll(testNodes);
          command.add("--sim_threshold");
          command.add(String.valueOf(0.9));
          command.add("--log_collection_minute");
          command.add(String.valueOf(logCollectionMinute));
          command.add("--state_execution_id=" + context.getStateExecutionInstanceId());
          command.add("--log_analysis_save_url");
          command.add(logAnalysisSaveUrl);
          command.add("--log_analysis_get_url");
          command.add(logAnalysisGetUrl);

          for (int attempt = 0; attempt < PYTHON_JOB_RETRIES; attempt++) {
            final ProcessResult result = new ProcessExecutor(command)
                                             .redirectOutput(Slf4jStream
                                                                 .of(LoggerFactory.getLogger(getClass().getName() + "."
                                                                     + context.getStateExecutionInstanceId()))
                                                                 .asInfo())
                                             .execute();

            switch (result.getExitValue()) {
              case 0:
                splunkService.markProcessed(context.getStateExecutionInstanceId(), context.getAppId(), endTime);
                logger.info("Splunk analysis done for " + context.getStateExecutionInstanceId() + "for minute "
                    + logCollectionMinute);
                attempt += PYTHON_JOB_RETRIES;
                break;
              case 2:
                logger.warn("No test data from the deployed nodes " + context.getStateExecutionInstanceId()
                    + "for minute " + logCollectionMinute);
                attempt += PYTHON_JOB_RETRIES;
                break;
              default:
                logger.error("Splunk analysis failed for " + context.getStateExecutionInstanceId() + "for minute "
                    + logCollectionMinute + " trial: " + (attempt + 1));
                Thread.sleep(2000);
            }
          }

        } catch (Exception e) {
          Misc.error(logger,
              "Splunk analysis failed for " + context.getStateExecutionInstanceId() + "for minute "
                  + logCollectionMinute,
              e);
        }
      }
      logCollectionMinute++;

      // if no data generated till this time, generate a response
      final SplunkMLAnalysisSummary analysisSummary =
          splunkService.getAnalysisSummary(context.getStateExecutionInstanceId(), context.getAppId());
      if (analysisSummary == null) {
        generateAnalysisResponse(context, ExecutionStatus.RUNNING, "No data with given queries has been found yet.");
      }
    }
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (StringUtils.isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  public void setComparisonStrategy(String comparisonStrategy) {
    this.comparisonStrategy = comparisonStrategy;
  }
}
