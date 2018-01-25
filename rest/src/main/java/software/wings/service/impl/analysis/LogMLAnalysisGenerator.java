package software.wings.service.impl.analysis;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.UUIDGenerator;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskBuilder;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;

import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
public class LogMLAnalysisGenerator implements Runnable {
  public static final int PYTHON_JOB_RETRIES = 3;

  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(LogMLAnalysisGenerator.class);

  public static final String LOG_ML_ROOT = "SPLUNKML_ROOT";
  protected static final String LOG_ML_SHELL_FILE_NAME = "run_splunkml.sh";

  private final AnalysisContext context;
  private final String pythonScriptRoot;
  private final String accountId;
  private final String applicationId;
  private final String workflowId;
  private final String serviceId;
  private final Set<String> testNodes;
  private final Set<String> controlNodes;
  private final Set<String> queries;
  private int logAnalysisMinute;
  private AnalysisService analysisService;
  private LearningEngineService learningEngineService;

  public LogMLAnalysisGenerator(AnalysisContext context, int logAnalysisMinute, AnalysisService analysisService,
      LearningEngineService learningEngineService) {
    this.context = context;
    this.analysisService = analysisService;
    this.pythonScriptRoot = System.getenv(LOG_ML_ROOT);
    Preconditions.checkState(isNotBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");

    this.applicationId = context.getAppId();
    this.accountId = context.getAccountId();
    this.workflowId = context.getWorkflowId();
    this.serviceId = context.getServiceId();
    this.testNodes = context.getTestNodes();
    this.controlNodes = context.getControlNodes();
    //    if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
    //      this.controlNodes.removeAll(this.testNodes);
    //    }
    this.queries = context.getQueries();
    this.logAnalysisMinute = logAnalysisMinute;
    this.learningEngineService = learningEngineService;
  }

  @Override
  public void run() {
    generateAnalysis();
    logAnalysisMinute++;
  }

  private void generateAnalysis() {
    try {
      for (String query : queries) {
        String uuid = UUIDGenerator.getUuid();
        // TODO fix this
        if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            && !analysisService.isLogDataCollected(
                   applicationId, context.getStateExecutionId(), query, logAnalysisMinute, context.getStateType())) {
          logger.warn("No data collected for minute " + logAnalysisMinute + " for application: " + applicationId
              + " stateExecution: " + context.getStateExecutionId() + ". No ML analysis will be run this minute");
          continue;
        }

        final String lastWorkflowExecutionId = analysisService.getLastSuccessfulWorkflowExecutionIdWithLogs(
            context.getStateType(), applicationId, serviceId, query, workflowId);
        final boolean isBaselineCreated =
            context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            || !lastWorkflowExecutionId.equals("-1");

        String testInputUrl = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL
            + "?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L2.name()
            + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&compareCurrent=true";

        String controlInputUrl;

        if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
          controlInputUrl = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL
              + "?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L2.name()
              + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&compareCurrent=true";
        } else {
          controlInputUrl = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL
              + "?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L2.name()
              + "&workflowExecutionId=" + lastWorkflowExecutionId + "&compareCurrent=false";
        }

        final String logAnalysisSaveUrl = "/api/" + context.getStateBaseUrl()
            + LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL + "?accountId=" + accountId
            + "&applicationId=" + applicationId + "&stateExecutionId=" + context.getStateExecutionId()
            + "&logCollectionMinute=" + logAnalysisMinute + "&isBaselineCreated=" + isBaselineCreated
            + "&taskId=" + uuid;
        final String logAnalysisGetUrl = "/api/" + context.getStateBaseUrl()
            + LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL + "?accountId=" + accountId;

        LearningEngineAnalysisTaskBuilder analysisTaskBuilder =
            LearningEngineAnalysisTask.builder()
                .ml_shell_file_name(LOG_ML_SHELL_FILE_NAME)
                .query(Sets.newHashSet(query.split(" ")))
                .workflow_id(context.getWorkflowId())
                .workflow_execution_id(context.getWorkflowExecutionId())
                .state_execution_id(context.getStateExecutionId())
                .service_id(context.getServiceId())
                .sim_threshold(0.9)
                .analysis_minute(logAnalysisMinute)
                .analysis_save_url(logAnalysisSaveUrl)
                .log_analysis_get_url(logAnalysisGetUrl)
                .ml_analysis_type(MLAnalysisType.LOG_ML)
                .stateType(context.getStateType());

        if (isBaselineCreated) {
          analysisTaskBuilder.control_input_url(controlInputUrl)
              .test_input_url(testInputUrl)
              .control_nodes(controlNodes)
              .test_nodes(testNodes);
        } else {
          analysisTaskBuilder.control_input_url(testInputUrl).control_nodes(testNodes);
        }

        LearningEngineAnalysisTask analysisTask = analysisTaskBuilder.build();
        analysisTask.setAppId(applicationId);
        analysisTask.setUuid(uuid);
        learningEngineService.addLearningEngineAnalysisTask(analysisTask);

        //        final List<String> command = new ArrayList<>();
        //        command.add(this.pythonScriptRoot + "/" + LOG_ML_SHELL_FILE_NAME);
        //        command.add("--query");
        //        command.addAll(Lists.newArrayList(query.split(" ")));
        //        if (isBaselineCreated) {
        //          command.add("--control_input_url");
        //          command.add(controlInputUrl);
        //          command.add("--test_input_url");
        //          command.add(testInputUrl);
        //          command.add("--control_nodes");
        //          command.addAll(controlNodes);
        //          command.add("--test_nodes");
        //          command.addAll(testNodes);
        //        } else {
        //          command.add("--control_input_url");
        //          command.add(testInputUrl);
        //          command.add("--control_nodes");
        //          command.addAll(testNodes);
        //        }
        //        command.add("--auth_token=" + context.getAuthToken());
        //        command.add("--application_id=" + applicationId);
        //        command.add("--workflow_id=" + workflowId);
        //        command.add("--service_id=" + serviceId);
        //        command.add("--sim_threshold");
        //        command.add(String.valueOf(0.9));
        //        command.add("--log_collection_minute");
        //        command.add(String.valueOf(logAnalysisMinute));
        //        command.add("--state_execution_id=" + context.getStateExecutionId());
        //        command.add("--log_analysis_save_url");
        //        command.add(logAnalysisSaveUrl);
        //        command.add("--log_analysis_get_url");
        //        command.add(logAnalysisGetUrl);
        //
        //        int attempt = 0;
        //        for (; attempt < PYTHON_JOB_RETRIES; attempt++) {
        //          final ProcessResult result =
        //              new ProcessExecutor(command)
        //                  .redirectOutput(
        //                      Slf4jStream
        //                          .of(LoggerFactory.getLogger(getClass().getName() + "." +
        //                          context.getStateExecutionId())) .asInfo())
        //                  .execute();
        //
        //          switch (result.getExitValue()) {
        //            case 0:
        //              logger.info(
        //                  "Log analysis done for " + context.getStateExecutionId() + " for minute " +
        //                  logAnalysisMinute);
        //              attempt += PYTHON_JOB_RETRIES;
        //              break;
        //            case 200:
        //              logger.warn("No control and test data from the deployed nodes " + context.getStateExecutionId()
        //                  + " for minute " + logAnalysisMinute);
        //              attempt += PYTHON_JOB_RETRIES;
        //              break;
        //            default:
        //              logger.warn("Log analysis failed for " + context.getStateExecutionId() + " for minute "
        //                  + logAnalysisMinute + " trial: " + (attempt + 1));
        //              Thread.sleep(2000);
        //          }
        //        }
        //
        //        if (attempt == PYTHON_JOB_RETRIES) {
        //          throw new RuntimeException("Finished all retries.");
        //        }
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Log analysis failed for " + context.getStateExecutionId() + " for minute " + logAnalysisMinute, e);
    }
  }
}
