package software.wings.service.impl.analysis;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.states.AbstractLogAnalysisState;

/**
 * Created by sriram_parthasarathy on 8/24/17.
 */
public class LogMLClusterGenerator implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(LogMLClusterGenerator.class);
  private static final String CLUSTER_ML_SHELL_FILE_NAME = "run_cluster_log_ml.sh";

  private LearningEngineService learningEngineService;

  private final LogClusterContext context;
  private final ClusterLevel fromLevel;
  private final ClusterLevel toLevel;
  private final LogRequest logRequest;
  private final String pythonScriptRoot;

  public LogMLClusterGenerator(LearningEngineService learningEngineService, LogClusterContext context,
      ClusterLevel fromLevel, ClusterLevel toLevel, LogRequest logRequest) {
    this.learningEngineService = learningEngineService;
    this.context = context;
    this.fromLevel = fromLevel;
    this.toLevel = toLevel;
    this.logRequest = logRequest;
    this.pythonScriptRoot = System.getenv(AbstractLogAnalysisState.LOG_ML_ROOT);
    Preconditions.checkState(isNotBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");
  }

  @Override
  public void run() {
    try {
      final String inputLogsUrl = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL
          + "?accountId=" + context.getAccountId() + "&workflowExecutionId=" + context.getWorkflowExecutionId()
          + "&compareCurrent=true&clusterLevel=" + fromLevel.name();
      String clusteredLogSaveUrl = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL
          + "?accountId=" + context.getAccountId() + "&stateExecutionId=" + context.getStateExecutionId()
          + "&workflowId=" + context.getWorkflowId() + "&workflowExecutionId=" + context.getWorkflowExecutionId()
          + "&serviceId=" + context.getServiceId() + "&appId=" + context.getAppId() + "&clusterLevel=" + toLevel.name();

      LearningEngineAnalysisTask analysisTask = LearningEngineAnalysisTask.builder()
                                                    .ml_shell_file_name(CLUSTER_ML_SHELL_FILE_NAME)
                                                    .control_input_url(inputLogsUrl)
                                                    .analysis_save_url(clusteredLogSaveUrl)
                                                    .workflow_id(context.getWorkflowId())
                                                    .workflow_execution_id(context.getWorkflowExecutionId())
                                                    .state_execution_id(context.getStateExecutionId())
                                                    .service_id(context.getServiceId())
                                                    .control_nodes(logRequest.getNodes())
                                                    .sim_threshold(0.99)
                                                    .analysis_minute(logRequest.getLogCollectionMinute())
                                                    .cluster_level(toLevel.getLevel())
                                                    .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                                                    .stateType(context.getStateType())
                                                    .query(Lists.newArrayList(logRequest.getQuery().split(" ")))
                                                    .build();
      analysisTask.setAppId(context.getAppId());

      final boolean taskQueued = learningEngineService.addLearningEngineAnalysisTask(analysisTask);
      if (taskQueued) {
        logger.info("Clustering queued for {}", logRequest);
      }
      //
      //      final List<String> command = new ArrayList<>();
      //      command.add(this.pythonScriptRoot + "/" + CLUSTER_ML_SHELL_FILE_NAME);
      //      command.add("--input_url");
      //      command.add(inputLogsUrl);
      //      command.add("--output_url");
      //      command.add(clusteredLogSaveUrl);
      //      command.add("--auth_token=" + context.getAuthToken());
      //      command.add("--application_id=" + context.getAppId());
      //      command.add("--workflow_id=" + context.getWorkflowId());
      //      command.add("--state_execution_id=" + context.getStateExecutionId());
      //      command.add("--service_id=" + context.getServiceId());
      //      command.add("--nodes");
      //      command.addAll(logRequest.getNodes());
      //      command.add("--sim_threshold");
      //      command.add(String.valueOf(0.99));
      //      command.add("--log_collection_minute");
      //      command.add(String.valueOf(logRequest.getLogCollectionMinute()));
      //      command.add("--cluster_level");
      //      command.add(String.valueOf(toLevel.getLevel()));
      //      command.add("--query");
      //      command.addAll(Lists.newArrayList(logRequest.getQuery().split(" ")));
      //
      //      final ProcessResult result =
      //          new ProcessExecutor(command)
      //              .redirectOutput(
      //                  Slf4jStream.of(LoggerFactory.getLogger(getClass().getName() + "." +
      //                  context.getStateExecutionId()))
      //                      .asInfo())
      //              .execute();
      //
      //      switch (result.getExitValue()) {
      //        case 0:
      //          logger.info("Clustering done for " + logRequest);
      //          break;
      //        case 2:
      //          logger.info("Clustering skipped. Nothing to process " + logRequest);
      //          break;
      //        default:
      //          throw new RuntimeException("Python Cluster job failed ");
      //      }

    } catch (Exception e) {
      throw new RuntimeException("First level clustering failed for " + logRequest, e);
    }
  }
}
