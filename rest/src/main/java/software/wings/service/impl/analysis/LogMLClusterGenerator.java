package software.wings.service.impl.analysis;

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;

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

  public LogMLClusterGenerator(LearningEngineService learningEngineService, LogClusterContext context,
      ClusterLevel fromLevel, ClusterLevel toLevel, LogRequest logRequest) {
    this.learningEngineService = learningEngineService;
    this.context = context;
    this.fromLevel = fromLevel;
    this.toLevel = toLevel;
    this.logRequest = logRequest;
  }

  @Override
  public void run() {
    final String inputLogsUrl = "/api/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + context.getAccountId()
        + "&workflowExecutionId=" + context.getWorkflowExecutionId()
        + "&compareCurrent=true&clusterLevel=" + fromLevel.name() + "&stateType=" + context.getStateType();
    String clusteredLogSaveUrl = "/api/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL + "?accountId=" + context.getAccountId()
        + "&stateExecutionId=" + context.getStateExecutionId() + "&workflowId=" + context.getWorkflowId()
        + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&serviceId=" + context.getServiceId()
        + "&appId=" + context.getAppId() + "&clusterLevel=" + toLevel.name() + "&stateType=" + context.getStateType();

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
  }
}
