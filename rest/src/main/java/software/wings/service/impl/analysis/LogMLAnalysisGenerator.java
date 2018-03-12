package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskBuilder;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask.LearningEngineExperimentalAnalysisTaskBuilder;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;

import java.util.List;
import java.util.Set;

/**
 * Created by sriram_parthasarathy on 8/23/17.
 */
public class LogMLAnalysisGenerator implements Runnable {
  @SchemaIgnore @Transient private static final Logger logger = LoggerFactory.getLogger(LogMLAnalysisGenerator.class);

  protected static final String LOG_ML_SHELL_FILE_NAME = "run_splunkml.sh";

  private final AnalysisContext context;
  private final String accountId;
  private final String applicationId;
  private final String workflowId;
  private final String serviceId;
  private final Set<String> testNodes;
  private final Set<String> controlNodes;
  private final Set<String> queries;
  private final boolean createExperiment;
  private int logAnalysisMinute;
  private AnalysisService analysisService;
  private LearningEngineService learningEngineService;

  public LogMLAnalysisGenerator(AnalysisContext context, int logAnalysisMinute, boolean createExperiment,
      AnalysisService analysisService, LearningEngineService learningEngineService) {
    this.context = context;
    this.analysisService = analysisService;
    this.applicationId = context.getAppId();
    this.accountId = context.getAccountId();
    this.workflowId = context.getWorkflowId();
    this.serviceId = context.getServiceId();
    this.testNodes = context.getTestNodes();
    this.controlNodes = context.getControlNodes();
    this.queries = context.getQueries();
    this.logAnalysisMinute = logAnalysisMinute;
    this.learningEngineService = learningEngineService;
    this.createExperiment = createExperiment;
  }

  @Override
  public void run() {
    generateAnalysis();
    logAnalysisMinute++;
  }

  private void generateAnalysis() {
    try {
      for (String query : queries) {
        String uuid = generateUuid();
        // TODO fix this
        if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            && !analysisService.isLogDataCollected(
                   applicationId, context.getStateExecutionId(), query, logAnalysisMinute, context.getStateType())) {
          logger.warn("No data collected for minute " + logAnalysisMinute + " for application: " + applicationId
              + " stateExecution: " + context.getStateExecutionId() + ". No ML analysis will be run this minute");
          continue;
        }

        final String lastWorkflowExecutionId = context.getPrevWorkflowExecutionId();
        final boolean isBaselineCreated =
            context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
            || !isEmpty(lastWorkflowExecutionId);

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

        String logAnalysisSaveUrl = "/api/" + context.getStateBaseUrl()
            + LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL + "?accountId=" + accountId
            + "&applicationId=" + applicationId + "&stateExecutionId=" + context.getStateExecutionId()
            + "&logCollectionMinute=" + logAnalysisMinute + "&isBaselineCreated=" + isBaselineCreated
            + "&taskId=" + uuid;

        if (!isEmpty(context.getPrevWorkflowExecutionId())) {
          logAnalysisSaveUrl += "&baseLineExecutionId=" + context.getPrevWorkflowExecutionId();
        }

        final String logAnalysisGetUrl = "/api/" + context.getStateBaseUrl()
            + LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL + "?accountId=" + accountId;

        String feedback_url = "";

        if (logAnalysisMinute == 0) {
          feedback_url = "/api/" + context.getStateBaseUrl() + LogAnalysisResource.ANALYSIS_USER_FEEDBACK
              + "?accountId=" + accountId + "&serviceId=" + serviceId + "&workflowId=" + workflowId
              + "&workflowExecutionId=" + context.getWorkflowExecutionId();
        }

        if (createExperiment) {
          final String experimentalLogAnalysisSaveUrl = "/api/learning-exp"
              + LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL + "?accountId=" + accountId
              + "&applicationId=" + applicationId + "&stateExecutionId=" + context.getStateExecutionId()
              + "&logCollectionMinute=" + logAnalysisMinute + "&isBaselineCreated=" + isBaselineCreated
              + "&taskId=" + uuid + "&stateType=" + context.getStateType();

          List<MLExperiments> experiments = learningEngineService.getExperiments(MLAnalysisType.LOG_ML);

          LearningEngineExperimentalAnalysisTaskBuilder experimentalAnalysisTaskBuilder =
              LearningEngineExperimentalAnalysisTask.builder()
                  .ml_shell_file_name(LOG_ML_SHELL_FILE_NAME)
                  .query(Lists.newArrayList(query.split(" ")))
                  .workflow_id(context.getWorkflowId())
                  .workflow_execution_id(context.getWorkflowExecutionId())
                  .state_execution_id(context.getStateExecutionId())
                  .service_id(context.getServiceId())
                  .sim_threshold(0.9)
                  .analysis_minute(logAnalysisMinute)
                  .analysis_save_url(experimentalLogAnalysisSaveUrl)
                  .log_analysis_get_url(logAnalysisGetUrl)
                  .ml_analysis_type(MLAnalysisType.LOG_ML)
                  .stateType(context.getStateType());

          if (!isEmpty(feedback_url)) {
            experimentalAnalysisTaskBuilder.feedback_url(feedback_url);
          }
          if (isBaselineCreated) {
            experimentalAnalysisTaskBuilder.control_input_url(controlInputUrl)
                .test_input_url(testInputUrl)
                .control_nodes(controlNodes)
                .test_nodes(testNodes);
          } else {
            experimentalAnalysisTaskBuilder.control_input_url(testInputUrl).control_nodes(testNodes);
          }

          LearningEngineExperimentalAnalysisTask experimentalAnalysisTask;
          for (MLExperiments experiment : experiments) {
            experimentalAnalysisTask =
                experimentalAnalysisTaskBuilder.experiment_name(experiment.getExperimentName()).build();
            experimentalAnalysisTask.setAppId(applicationId);
            experimentalAnalysisTask.setUuid(uuid);
            learningEngineService.addLearningEngineExperimentalAnalysisTask(experimentalAnalysisTask);
          }
        }

        LearningEngineAnalysisTaskBuilder analysisTaskBuilder =
            LearningEngineAnalysisTask.builder()
                .ml_shell_file_name(LOG_ML_SHELL_FILE_NAME)
                .query(Lists.newArrayList(query.split(" ")))
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

        if (!isEmpty(feedback_url)) {
          analysisTaskBuilder.feedback_url(feedback_url);
        }

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
      }

    } catch (Exception e) {
      throw new RuntimeException(
          "Log analysis failed for " + context.getStateExecutionId() + " for minute " + logAnalysisMinute, e);
    }
  }
}
