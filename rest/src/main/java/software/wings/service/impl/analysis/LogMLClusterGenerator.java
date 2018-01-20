package software.wings.service.impl.analysis;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.states.AbstractLogAnalysisState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sriram_parthasarathy on 8/24/17.
 */
public class LogMLClusterGenerator implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(LogMLClusterGenerator.class);
  private static final String CLUSTER_ML_SHELL_FILE_NAME = "run_cluster_log_ml.sh";

  @Inject protected AnalysisService analysisService;

  private final LogClusterContext context;
  private final ClusterLevel fromLevel;
  private final ClusterLevel toLevel;
  private final LogRequest logRequest;
  private final String serverUrl;
  private final String pythonScriptRoot;

  public LogMLClusterGenerator(
      LogClusterContext context, ClusterLevel fromLevel, ClusterLevel toLevel, LogRequest logRequest) {
    this.context = context;
    this.fromLevel = fromLevel;
    this.toLevel = toLevel;
    this.logRequest = logRequest;
    String protocol = context.isSSL() ? "https" : "http";
    this.serverUrl = protocol + "://localhost:" + context.getAppPort();
    this.pythonScriptRoot = System.getenv(AbstractLogAnalysisState.LOG_ML_ROOT);
    Preconditions.checkState(isNotBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");
  }

  @Override
  public void run() {
    try {
      final String inputLogsUrl = this.serverUrl + "/api/" + context.getStateBaseUrl()
          + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + context.getAccountId()
          + "&workflowExecutionId=" + context.getWorkflowExecutionId()
          + "&compareCurrent=true&clusterLevel=" + fromLevel.name();
      String clusteredLogSaveUrl = this.serverUrl + "/api/" + context.getStateBaseUrl()
          + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL + "?accountId=" + context.getAccountId()
          + "&stateExecutionId=" + context.getStateExecutionId() + "&workflowId=" + context.getWorkflowId()
          + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&serviceId=" + context.getServiceId()
          + "&appId=" + context.getAppId() + "&clusterLevel=" + toLevel.name();

      final List<String> command = new ArrayList<>();
      command.add(this.pythonScriptRoot + "/" + CLUSTER_ML_SHELL_FILE_NAME);
      command.add("--input_url");
      command.add(inputLogsUrl);
      command.add("--output_url");
      command.add(clusteredLogSaveUrl);
      command.add("--auth_token=" + context.getAuthToken());
      command.add("--application_id=" + context.getAppId());
      command.add("--workflow_id=" + context.getWorkflowId());
      command.add("--state_execution_id=" + context.getStateExecutionId());
      command.add("--service_id=" + context.getServiceId());
      command.add("--nodes");
      command.addAll(logRequest.getNodes());
      command.add("--sim_threshold");
      command.add(String.valueOf(0.99));
      command.add("--log_collection_minute");
      command.add(String.valueOf(logRequest.getLogCollectionMinute()));
      command.add("--cluster_level");
      command.add(String.valueOf(toLevel.getLevel()));
      command.add("--query");
      command.addAll(Lists.newArrayList(logRequest.getQuery().split(" ")));

      final ProcessResult result =
          new ProcessExecutor(command)
              .redirectOutput(
                  Slf4jStream.of(LoggerFactory.getLogger(getClass().getName() + "." + context.getStateExecutionId()))
                      .asInfo())
              .execute();

      switch (result.getExitValue()) {
        case 0:
          logger.info("Clustering done for " + logRequest);
          break;
        case 2:
          logger.info("Clustering skipped. Nothing to process " + logRequest);
          break;
        default:
          throw new RuntimeException("Python Cluster job failed ");
      }

    } catch (Exception e) {
      throw new RuntimeException("First level clustering failed for " + logRequest, e);
    }
  }
}
