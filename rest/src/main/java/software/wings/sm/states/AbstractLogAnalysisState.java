package software.wings.sm.states;

import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
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
 * Created by rsingh on 7/6/17.
 */
public abstract class AbstractLogAnalysisState extends AbstractAnalysisState {
  public static final String LOG_ML_ROOT = "SPLUNKML_ROOT";
  protected static final String LOG_ML_SHELL_FILE_NAME = "run_splunkml.sh";

  protected String query;

  @Transient @SchemaIgnore protected ScheduledExecutorService pythonExecutorService;

  @Attributes(required = true, title = "Search Keywords", description = "Such as *Exception*")
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

      getLogger().warn(
          "It seems that there is no successful run for this workflow yet. Log data will be collected to be analyzed for next deployment run");
    }

    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
        && lastExecutionNodes.equals(canaryNewHostNames)) {
      getLogger().error("Control and test nodes are same. Will not be running Log analysis");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED, getAnalysisServerConfigId(),
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
            .withCorrelationId(UUID.randomUUID().toString())
            .build();

    Set<String> hostsToBeCollected = new HashSet<>();
    if (lastExecutionNodes != null) {
      hostsToBeCollected.addAll(lastExecutionNodes);
    }
    hostsToBeCollected.addAll(canaryNewHostNames);
    triggerAnalysisDataCollection(context, hostsToBeCollected);

    final LogAnalysisResponse response = aLogAnalysisResponse()
                                             .withLogAnalysisExecutionData(executionData)
                                             .withExecutionStatus(ExecutionStatus.SUCCESS)
                                             .build();
    pythonExecutorService = createPythonExecutorService(context, response);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
        .withExecutionStatus(ExecutionStatus.RUNNING)
        .withErrorMessage("Log Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  private void shutDownGenerator(LogAnalysisResponse response) {
    waitNotifyEngine.notify(response.getLogAnalysisExecutionData().getCorrelationId(), response);
    pythonExecutorService.shutdown();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
        context.getStateExecutionInstanceId(), context.getAppId(), StateType.valueOf(getStateType()));
    if (analysisSummary == null) {
      getLogger().warn("No analysis summary. This can happen if there is no data with the given queries");
      return generateAnalysisResponse(context, ExecutionStatus.SUCCESS, getAnalysisServerConfigId(),
          "No data found with given queries. Skipped Analysis");
    }

    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    if (analysisSummary.getRiskLevel() == RiskLevel.HIGH) {
      getLogger().info(analysisSummary.getAnalysisSummaryMessage() + " Marking it failed.");
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
      if (pythonExecutorService != null) {
        pythonExecutorService.shutdown();
        pythonExecutorService.awaitTermination(1, TimeUnit.MINUTES);
      }
    } catch (InterruptedException e) {
      pythonExecutorService.shutdown();
    }

    final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
        context.getStateExecutionInstanceId(), context.getAppId(), StateType.valueOf(getStateType()));

    if (analysisSummary == null) {
      generateAnalysisResponse(
          context, ExecutionStatus.ABORTED, getAnalysisServerConfigId(), "Workflow was aborted while analysing");
    }
  }

  private ScheduledExecutorService createPythonExecutorService(ExecutionContext context, LogAnalysisResponse response) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new LogMLAnalysisGenerator(context, response), SplunkDataCollectionTask.DELAY_MINUTES + 1, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

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
    for (String logQuery : query.split(",")) {
      final LogMLAnalysisRecord analysisRecord = new LogMLAnalysisRecord();
      analysisRecord.setStateType(StateType.valueOf(getStateType()));
      analysisRecord.setApplicationId(context.getAppId());
      analysisRecord.setStateExecutionId(context.getStateExecutionInstanceId());
      executionData.setStatus(status);
      analysisRecord.setQuery(logQuery);
      analysisRecord.setAnalysisSummaryMessage(message);
      analysisRecord.setControl_events(Collections.emptyMap());
      analysisRecord.setTest_events(Collections.emptyMap());
      analysisService.saveLogAnalysisRecords(analysisRecord, StateType.valueOf(getStateType()));
    }

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

  private class LogMLAnalysisGenerator implements Runnable {
    private final ExecutionContext context;
    private final LogAnalysisResponse logAnalysisResponse;
    private final String pythonScriptRoot;
    private final String serverUrl;
    private final String accountId;
    private final String applicationId;
    private final String workflowId;
    private final String serviceId;
    private final Set<String> testNodes;
    private final Set<String> controlNodes;
    private final Set<String> queries;
    private int logAnalysisMinute = 0;

    public LogMLAnalysisGenerator(ExecutionContext context, LogAnalysisResponse logAnalysisResponse) {
      this.context = context;
      this.logAnalysisResponse = logAnalysisResponse;
      this.pythonScriptRoot = System.getenv(LOG_ML_ROOT);
      Preconditions.checkState(!StringUtils.isBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");

      String protocol = AbstractLogAnalysisState.this.configuration.isSslEnabled() ? "https" : "http";
      this.serverUrl = protocol + "://localhost:" + AbstractLogAnalysisState.this.configuration.getApplicationPort();
      this.applicationId = context.getAppId();
      this.accountId = AbstractLogAnalysisState.this.appService.get(this.applicationId).getAccountId();
      this.workflowId = getWorkflowId(context);
      this.serviceId = getPhaseServiceId(context);
      this.testNodes = getCanaryNewHostNames(context);
      this.controlNodes = getLastExecutionNodes(context);
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        this.controlNodes.removeAll(this.testNodes);
      }
      this.queries = Sets.newHashSet(query.split(","));
    }

    @Override
    public void run() {
      if (logAnalysisMinute > Integer.parseInt(timeDuration)) {
        return;
      }

      preProcess(context, logAnalysisMinute);
      generateAnalysis();
      logAnalysisMinute++;

      // if no data generated till this time, generate a response
      final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
          context.getStateExecutionInstanceId(), context.getAppId(), StateType.valueOf(getStateType()));
      if (analysisSummary == null) {
        generateAnalysisResponse(context, ExecutionStatus.RUNNING, getAnalysisServerConfigId(),
            "No data with given queries has been found yet.");
      }

      if (logAnalysisMinute >= Integer.parseInt(timeDuration)) {
        shutDownGenerator(logAnalysisResponse);
      }
    }

    private void generateAnalysis() {
      final StateType stateType = StateType.valueOf(getStateType());
      for (String query : queries) {
        if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            && !analysisService.isLogDataCollected(applicationId, context.getStateExecutionInstanceId(), query,
                   logAnalysisMinute, StateType.valueOf(getStateType()))) {
          getLogger().warn("No data collected for minute " + logAnalysisMinute + " for application: " + applicationId
              + " stateExecution: " + context.getStateExecutionInstanceId()
              + ". No ML analysis will be run this minute");
          continue;
        }

        try {
          final boolean isBaselineCreated = analysisService.isBaselineCreated(getComparisonStrategy(), stateType,
              applicationId, workflowId, context.getWorkflowExecutionId(), serviceId, query);
          String testInputUrl = this.serverUrl + "/api/" + getStateBaseUrl(stateType)
              + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + accountId
              + "&clusterLevel=" + ClusterLevel.L2.name() + "&compareCurrent=true";
          String controlInputUrl = this.serverUrl + "/api/" + getStateBaseUrl(stateType)
              + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + accountId
              + "&clusterLevel=" + ClusterLevel.L2.name() + "&compareCurrent=";
          controlInputUrl = getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
              ? controlInputUrl + true
              : controlInputUrl + false;

          final String logAnalysisSaveUrl = this.serverUrl + "/api/" + getStateBaseUrl(stateType)
              + LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL + "?accountId=" + accountId
              + "&applicationId=" + applicationId + "&stateExecutionId=" + context.getStateExecutionInstanceId();
          final String logAnalysisGetUrl = this.serverUrl + "/api/" + getStateBaseUrl(stateType)
              + LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL + "?accountId=" + accountId;
          final List<String> command = new ArrayList<>();
          command.add(this.pythonScriptRoot + "/" + LOG_ML_SHELL_FILE_NAME);
          command.add("--query=" + query);
          if (isBaselineCreated) {
            command.add("--control_input_url");
            command.add(controlInputUrl);
            command.add("--test_input_url");
            command.add(testInputUrl);
            command.add("--control_nodes");
            command.addAll(controlNodes);
            command.add("--test_nodes");
            command.addAll(testNodes);
          } else {
            command.add("--control_input_url");
            command.add(testInputUrl);
            command.add("--control_nodes");
            command.addAll(testNodes);
          }
          command.add("--auth_token=" + generateAuthToken());
          command.add("--application_id=" + applicationId);
          command.add("--workflow_id=" + workflowId);
          command.add("--service_id=" + serviceId);
          command.add("--sim_threshold");
          command.add(String.valueOf(0.9));
          command.add("--log_collection_minute");
          command.add(String.valueOf(logAnalysisMinute));
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
                getLogger().info("Log analysis done for " + context.getStateExecutionInstanceId() + "for minute "
                    + logAnalysisMinute);
                attempt += PYTHON_JOB_RETRIES;
                break;
              case 2:
                getLogger().warn("No test data from the deployed nodes " + context.getStateExecutionInstanceId()
                    + "for minute " + logAnalysisMinute);
                attempt += PYTHON_JOB_RETRIES;
                break;
              default:
                getLogger().error("Log analysis failed for " + context.getStateExecutionInstanceId() + "for minute "
                    + logAnalysisMinute + " trial: " + (attempt + 1));
                Thread.sleep(2000);
            }
          }

        } catch (Exception e) {
          getLogger().error(
              "Log analysis failed for " + context.getStateExecutionInstanceId() + "for minute " + logAnalysisMinute,
              e);
        }
      }
    }
  }

  protected abstract void preProcess(ExecutionContext context, int logAnalysisMinute);
}
