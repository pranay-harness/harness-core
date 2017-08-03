package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

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
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkSettingProvider;
import software.wings.service.impl.analysis.LogCollectionCallback;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
public class ElkAnalysisState extends AbstractLogAnalysisState {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(ElkAnalysisState.class);

  private static final String SPLUNKML_ROOT = "SPLUNKML_ROOT";
  private static final String SPLUNKML_SHELL_FILE_NAME = "run_splunkml.sh";

  @EnumData(enumDataProvider = ElkSettingProvider.class)
  @Attributes(required = true, title = "Elastic Search Server")
  private String analysisServerConfigId;

  public ElkAnalysisState(String name) {
    super(name, StateType.ELK.getType());
  }

  @Override
  protected void triggerAnalysisDataCollection(ExecutionContext context, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + analysisServerConfigId + " found");
    }

    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    final Set<String> queries = Sets.newHashSet(query.split(","));
    final long logCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final ElkDataCollectionInfo dataCollectionInfo = new ElkDataCollectionInfo(
        appService.get(context.getAppId()).getAccountId(), context.getAppId(), context.getStateExecutionInstanceId(),
        getWorkflowId(context), elkConfig, queries, hosts, logCollectionStartTimeStamp, Integer.parseInt(timeDuration));
    String waitId = UUIDGenerator.getUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.ELK_COLLECT_LOG_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .build();
    waitNotifyEngine.waitForAll(new LogCollectionCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Override
  @SchemaIgnore
  protected Runnable getLogAnanlysisGenerator(ExecutionContext context) {
    return new ElkAnalysisGenerator(context);
  }

  private class ElkAnalysisGenerator implements Runnable {
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

    public ElkAnalysisGenerator(ExecutionContext context) {
      this.context = context;
      this.pythonScriptRoot = System.getenv(SPLUNKML_ROOT);
      Preconditions.checkState(!StringUtils.isBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");

      String protocol = ElkAnalysisState.this.configuration.isSslEnabled() ? "https" : "http";
      this.serverUrl = protocol + "://localhost:" + ElkAnalysisState.this.configuration.getApplicationPort();
      this.applicationId = context.getAppId();
      this.accountId = ElkAnalysisState.this.appService.get(this.applicationId).getAccountId();
      this.workflowId = getWorkflowId(context);
      this.testNodes = getCanaryNewHostNames(context);
      this.controlNodes = getLastExecutionNodes(context);
      this.controlNodes.removeAll(this.testNodes);
      this.queries = Sets.newHashSet(query.split(","));
    }

    @Override
    public void run() {
      if (true) {
        return;
      }
      if (logCollectionMinute > Integer.parseInt(timeDuration)) {
        return;
      }

      for (String query : queries) {
        if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            && !analysisService.isLogDataCollected(applicationId, context.getStateExecutionInstanceId(), query,
                   logCollectionMinute, StateType.SPLUNKV2)) {
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
                analysisService.markProcessed(
                    context.getStateExecutionInstanceId(), context.getAppId(), endTime, StateType.SPLUNKV2);
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
      final LogMLAnalysisSummary analysisSummary = analysisService.getAnalysisSummary(
          context.getStateExecutionInstanceId(), context.getAppId(), StateType.SPLUNKV2);
      if (analysisSummary == null) {
        generateAnalysisResponse(
            context, ExecutionStatus.RUNNING, analysisServerConfigId, "No data with given queries has been found yet.");
      }
    }
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
  }
}
