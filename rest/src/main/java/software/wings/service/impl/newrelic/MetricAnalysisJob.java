package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.delegatetasks.NewRelicDataCollectionTask;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.metrics.MetricType;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Created by rsingh on 9/11/17.
 */
@DisallowConcurrentExecution
public class MetricAnalysisJob implements Job {
  @Inject private MetricDataAnalysisService analysisService;

  @Inject private LearningEngineService learningEngineService;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject private DelegateService delegateService;

  @Inject private FeatureFlagService featureFlagService;

  private static final Logger logger = LoggerFactory.getLogger(MetricAnalysisJob.class);
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      long timestamp = jobExecutionContext.getMergedJobDataMap().getLong("timestamp");
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      String delegateTaskId = jobExecutionContext.getMergedJobDataMap().getString("delegateTaskId");

      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      new MetricAnalysisGenerator(analysisService, learningEngineService, waitNotifyEngine, delegateService, context,
          jobExecutionContext, delegateTaskId)
          .run();

    } catch (Exception ex) {
      logger.warn("Log analysis cron failed with error", ex);
      try {
        jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
      } catch (SchedulerException e) {
        logger.error("Unable to clean up cron", e);
      }
    }
  }

  public static class MetricAnalysisGenerator implements Runnable {
    //    public static final int PYTHON_JOB_RETRIES = 3;
    public static final int ANALYSIS_DURATION = 30;
    protected static final String TS_ML_SHELL_FILE_NAME = "run_time_series_ml.sh";
    private static final int APM_BUFFER_MINUTES = 2;
    private final AnalysisContext context;
    private final JobExecutionContext jobExecutionContext;
    private final String delegateTaskId;
    private final Set<String> testNodes;
    private final Set<String> controlNodes;
    private final MetricDataAnalysisService analysisService;
    private final LearningEngineService learningEngineService;
    private final WaitNotifyEngine waitNotifyEngine;
    private final DelegateService delegateService;
    private final int analysisDuration;

    public MetricAnalysisGenerator(MetricDataAnalysisService service, LearningEngineService learningEngineService,
        WaitNotifyEngine waitNotifyEngine, DelegateService delegateService, AnalysisContext context,
        JobExecutionContext jobExecutionContext, String delegateTaskId) {
      this.analysisService = service;
      this.learningEngineService = learningEngineService;
      this.waitNotifyEngine = waitNotifyEngine;
      this.delegateService = delegateService;
      this.context = context;
      this.jobExecutionContext = jobExecutionContext;
      this.delegateTaskId = delegateTaskId;
      this.testNodes = context.getTestNodes();
      this.controlNodes = context.getControlNodes();
      if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        this.controlNodes.removeAll(this.testNodes);
      }
      this.analysisDuration = context.getTimeDuration() - APM_BUFFER_MINUTES - 1;
    }

    private Map<String, MetricType> getMetricTypeMap(Map<String, TimeSeriesMetricDefinition> stateValuesToAnalyze) {
      Map<String, MetricType> stateValuesToThresholds = new HashMap<>();
      for (Entry<String, TimeSeriesMetricDefinition> entry : stateValuesToAnalyze.entrySet()) {
        String metricName = entry.getValue().getMetricName();
        stateValuesToThresholds.put(metricName, entry.getValue().getMetricType());
      }

      return stateValuesToThresholds;
    }

    private NewRelicMetricAnalysisRecord analyzeLocal(int analysisMinute) {
      logger.info("running " + context.getStateType().name() + " for minute {}", analysisMinute);
      int analysisStartMin = analysisMinute > ANALYSIS_DURATION ? analysisMinute - ANALYSIS_DURATION : 0;
      final List<NewRelicMetricDataRecord> controlRecords =
          context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS
          ? analysisService.getPreviousSuccessfulRecords(context.getStateType(), context.getWorkflowId(),
                context.getServiceId(), analysisMinute, analysisStartMin)
          : analysisService.getRecords(context.getStateType(), context.getWorkflowExecutionId(),
                context.getStateExecutionId(), context.getWorkflowId(), context.getServiceId(),
                context.getControlNodes(), analysisMinute, analysisStartMin);

      final List<NewRelicMetricDataRecord> testRecords = analysisService.getRecords(context.getStateType(),
          context.getWorkflowExecutionId(), context.getStateExecutionId(), context.getWorkflowId(),
          context.getServiceId(), context.getTestNodes(), analysisMinute, analysisStartMin);

      Map<String, List<NewRelicMetricDataRecord>> controlRecordsByMetric = splitMetricsByName(controlRecords);
      Map<String, List<NewRelicMetricDataRecord>> testRecordsByMetric = splitMetricsByName(testRecords);

      NewRelicMetricAnalysisRecord analysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                        .stateType(context.getStateType())
                                                        .stateExecutionId(context.getStateExecutionId())
                                                        .workflowExecutionId(context.getWorkflowExecutionId())
                                                        .workflowId(context.getWorkflowId())
                                                        .applicationId(context.getAppId())
                                                        .riskLevel(RiskLevel.LOW)
                                                        .metricAnalyses(new ArrayList<>())
                                                        .build();

      Map<String, MetricType> stateValuesToAnalyze;
      switch (context.getStateType()) {
        case NEW_RELIC:
          stateValuesToAnalyze = getMetricTypeMap(NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE);
          break;
        case APP_DYNAMICS:
          stateValuesToAnalyze = getMetricTypeMap(NewRelicMetricValueDefinition.APP_DYNAMICS_VALUES_TO_ANALYZE);
          break;
        case DYNA_TRACE:
          stateValuesToAnalyze = getMetricTypeMap(DynaTraceTimeSeries.getDefinitionsToAnalyze());
          break;
        case PROMETHEUS:
        case CLOUD_WATCH:
        case DATA_DOG:
          stateValuesToAnalyze = getMetricTypeMap(
              analysisService.getMetricTemplates(context.getStateType(), context.getStateExecutionId()));
          break;
        default:
          throw new IllegalStateException("Invalid stateType " + context.getStateType());
      }

      for (Entry<String, List<NewRelicMetricDataRecord>> metric : testRecordsByMetric.entrySet()) {
        final String metricName = metric.getKey();
        NewRelicMetricAnalysis metricAnalysis = NewRelicMetricAnalysis.builder()
                                                    .metricName(metricName)
                                                    .riskLevel(RiskLevel.LOW)
                                                    .metricValues(new ArrayList<>())
                                                    .build();

        for (Entry<String, MetricType> valuesToAnalyze : stateValuesToAnalyze.entrySet()) {
          NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                    .metricName(metricName)
                                                                    .metricValueName(valuesToAnalyze.getKey())
                                                                    .metricType(valuesToAnalyze.getValue())
                                                                    .build();

          NewRelicMetricAnalysisValue metricAnalysisValue =
              metricValueDefinition.analyze(metric.getValue(), controlRecordsByMetric.get(metricName));
          metricAnalysis.addNewRelicMetricAnalysisValue(metricAnalysisValue);

          if (metricAnalysisValue.getRiskLevel().compareTo(metricAnalysis.getRiskLevel()) < 0) {
            metricAnalysis.setRiskLevel(metricAnalysisValue.getRiskLevel());
          }

          if (metricAnalysisValue.getRiskLevel().compareTo(analysisRecord.getRiskLevel()) < 0) {
            analysisRecord.setRiskLevel(metricAnalysisValue.getRiskLevel());
          }
        }
        analysisRecord.addNewRelicMetricAnalysis(metricAnalysis);
      }

      analysisRecord.setAnalysisMinute(analysisMinute);

      return analysisRecord;
    }

    private boolean timeSeriesML(int analysisMinute) throws InterruptedException, TimeoutException, IOException {
      if (learningEngineService.hasAnalysisTimedOut(
              context.getAppId(), context.getWorkflowExecutionId(), context.getStateExecutionId())) {
        learningEngineService.markStatus(
            context.getWorkflowExecutionId(), context.getStateExecutionId(), analysisMinute, ExecutionStatus.FAILED);
        throw new WingsException("Error running time series analysis. Finished all retries. stateExecutionId: "
            + context.getStateExecutionId());
      }
      int analysisStartMin = analysisMinute > ANALYSIS_DURATION ? analysisMinute - ANALYSIS_DURATION : 0;

      String testInputUrl = "/api/" + MetricDataAnalysisService.RESOURCE_URL
          + "/get-metrics?accountId=" + context.getAccountId() + "&stateType=" + context.getStateType()
          + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&compareCurrent=true";
      String controlInputUrl = "/api/" + MetricDataAnalysisService.RESOURCE_URL + "/get-metrics?accountId="
          + context.getAccountId() + "&stateType=" + context.getStateType() + "&compareCurrent=";
      if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        controlInputUrl = controlInputUrl + true + "&workflowExecutionId=" + context.getWorkflowExecutionId();
      } else {
        controlInputUrl = controlInputUrl + false + "&workflowExecutionId=" + context.getPrevWorkflowExecutionId();
      }

      String uuid = generateUuid();

      String logAnalysisSaveUrl = "/api/" + MetricDataAnalysisService.RESOURCE_URL
          + "/save-analysis?accountId=" + context.getAccountId() + "&stateType=" + context.getStateType()
          + "&applicationId=" + context.getAppId() + "&workflowExecutionId=" + context.getWorkflowExecutionId()
          + "&stateExecutionId=" + context.getStateExecutionId() + "&analysisMinute=" + analysisMinute
          + "&taskId=" + uuid + "&serviceId=" + context.getServiceId() + "&workflowId=" + context.getWorkflowId();

      if (!isEmpty(context.getPrevWorkflowExecutionId())) {
        logAnalysisSaveUrl += "&baseLineExecutionId=" + context.getPrevWorkflowExecutionId();
      }

      LearningEngineAnalysisTask learningEngineAnalysisTask =
          LearningEngineAnalysisTask.builder()
              .ml_shell_file_name(TS_ML_SHELL_FILE_NAME)
              .workflow_id(context.getWorkflowId())
              .workflow_execution_id(context.getWorkflowExecutionId())
              .state_execution_id(context.getStateExecutionId())
              .service_id(context.getServiceId())
              .auth_token(context.getAuthToken())
              .analysis_start_min(analysisStartMin)
              .analysis_minute(analysisMinute)
              .smooth_window(context.getSmooth_window())
              .tolerance(context.getTolerance())
              .min_rpm(context.getMinimumRequestsPerMinute())
              .comparison_unit_window(context.getComparisonWindow())
              .parallel_processes(context.getParallelProcesses())
              .test_input_url(testInputUrl)
              .control_input_url(controlInputUrl)
              .analysis_save_url(logAnalysisSaveUrl)
              .metric_template_url("/api/" + MetricDataAnalysisService.RESOURCE_URL
                  + "/get-metric-template?accountId=" + context.getAccountId() + "&stateType=" + context.getStateType()
                  + "&stateExecutionId=" + context.getStateExecutionId())
              .control_nodes(controlNodes)
              .test_nodes(testNodes)
              .stateType(context.getStateType())
              .ml_analysis_type(MLAnalysisType.TIME_SERIES)
              .build();
      learningEngineAnalysisTask.setAppId(context.getAppId());
      learningEngineAnalysisTask.setUuid(uuid);

      logger.info("Queueing for analysis {}", learningEngineAnalysisTask);
      return learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }

    @Override
    public void run() {
      logger.info("Starting analysis for " + context.getStateExecutionId());

      boolean completeCron = false;
      boolean error = false;
      String errMsg = "";

      try {
        /**
         * Work flow is invalid
         * exit immediately
         **/
        if (!analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
          completeCron = true;
          return;
        }

        final NewRelicMetricDataRecord heartBeatRecord = analysisService.getLastHeartBeat(context.getStateType(),
            context.getStateExecutionId(), context.getWorkflowExecutionId(), context.getServiceId());

        if (heartBeatRecord != null && heartBeatRecord.getDataCollectionMinute() >= analysisDuration) {
          logger.info(
              "time series analysis finished after running for {} minutes", heartBeatRecord.getDataCollectionMinute());
          completeCron = true;
          return;
        }

        final NewRelicMetricDataRecord analysisDataRecord = analysisService.getAnalysisMinute(context.getStateType(),
            context.getStateExecutionId(), context.getWorkflowExecutionId(), context.getServiceId());
        if (analysisDataRecord == null) {
          logger.info("Skipping time series analysis. No new data.");
          return;
        }
        int analysisMinute = analysisDataRecord.getDataCollectionMinute();

        logger.info("running analysis for " + context.getStateExecutionId() + " for minute" + analysisMinute);

        boolean runTimeSeriesML = true;

        boolean taskQueued = false;
        if (runTimeSeriesML) {
          switch (context.getComparisonStrategy()) {
            case COMPARE_WITH_PREVIOUS:
              if (isEmpty(context.getPrevWorkflowExecutionId())) {
                runTimeSeriesML = false;
                break;
              }
              int minControlMinute = analysisService.getMinControlMinuteWithData(context.getStateType(),
                  context.getServiceId(), context.getWorkflowId(), context.getPrevWorkflowExecutionId());

              if (analysisMinute < minControlMinute) {
                logger.info("Baseline control minute starts at " + minControlMinute
                    + ". But current analysis minute is  " + analysisMinute
                    + "Will run local analysis instead of ML for minute " + analysisMinute);
                runTimeSeriesML = false;
                break;
              }

              int maxControlMinute = analysisService.getMaxControlMinuteWithData(context.getStateType(),
                  context.getServiceId(), context.getWorkflowId(), context.getPrevWorkflowExecutionId());

              if (analysisMinute > maxControlMinute) {
                logger.warn("Not enough control data. analysis minute = " + analysisMinute
                    + " , max control minute = " + maxControlMinute);
                // Do nothing. Don't run any analysis.
                taskQueued = true;
                analysisService.bumpCollectionMinuteToProcess(context.getStateType(), context.getStateExecutionId(),
                    context.getWorkflowExecutionId(), context.getServiceId(), analysisMinute);
                break;
              }
              taskQueued = timeSeriesML(analysisMinute);
              break;
              // Note that control flows through to COMPARE_WITH_CURRENT where the ml analysis is run.
            case COMPARE_WITH_CURRENT:
              logger.info("running time series ml analysis for minute " + analysisMinute);
              taskQueued = timeSeriesML(analysisMinute);
              break;
            default:
              throw new IllegalStateException("Invalid type " + context.getComparisonStrategy());
          }
        }

        if (!runTimeSeriesML) {
          logger.info("running local time series analysis for {}", context.getStateExecutionId());
          NewRelicMetricAnalysisRecord analysisRecord = analyzeLocal(analysisMinute);
          analysisService.saveAnalysisRecords(analysisRecord);
          analysisService.bumpCollectionMinuteToProcess(context.getStateType(), context.getStateExecutionId(),
              context.getWorkflowExecutionId(), context.getServiceId(), analysisMinute);
        } else if (!taskQueued) {
          return;
        }
      } catch (Exception ex) {
        completeCron = true;
        error = true;
        errMsg = ex.getMessage();
        logger.warn("analysis failed", ex);
      } finally {
        try {
          // send notification to state manager and delete cron.
          if (completeCron || !analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            try {
              delegateService.abortTask(context.getAccountId(), delegateTaskId);
              sendStateNotification(context, error, errMsg);
            } catch (Exception e) {
              logger.error("Send notification failed for new relic analysis manager", e);
            } finally {
              try {
                jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
              } catch (Exception e) {
                logger.error("Delete cron failed", e);
              }
            }
          }
        } catch (Exception ex) {
          logger.error("analysis failed", ex);
        }
      }
    }

    private Map<String, List<NewRelicMetricDataRecord>> splitMetricsByName(List<NewRelicMetricDataRecord> records) {
      final Map<String, List<NewRelicMetricDataRecord>> rv = new HashMap<>();
      for (NewRelicMetricDataRecord record : records) {
        if (record.getName().equals(NewRelicDataCollectionTask.HARNESS_HEARTBEAT_METRIC_NAME)) {
          continue;
        }
        if (!rv.containsKey(record.getName())) {
          rv.put(record.getName(), new ArrayList<>());
        }

        rv.get(record.getName()).add(record);
      }

      return rv;
    }

    private void sendStateNotification(AnalysisContext context, boolean error, String errMsg) {
      if (analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
        final ExecutionStatus status = error ? ExecutionStatus.ERROR : ExecutionStatus.SUCCESS;
        final MetricAnalysisExecutionData executionData =
            MetricAnalysisExecutionData.builder()
                .workflowExecutionId(context.getWorkflowExecutionId())
                .stateExecutionInstanceId(context.getStateExecutionId())
                .serverConfigId(context.getAnalysisServerConfigId())
                .timeDuration(context.getTimeDuration())
                .canaryNewHostNames(context.getTestNodes())
                .lastExecutionNodes(context.getControlNodes() == null ? new HashSet<>() : context.getControlNodes())
                .correlationId(context.getCorrelationId())
                .build();
        executionData.setStatus(status);
        if (error) {
          executionData.setErrorMsg(errMsg);
        }
        final MetricDataAnalysisResponse response =
            MetricDataAnalysisResponse.builder().stateExecutionData(executionData).build();
        response.setExecutionStatus(status);
        logger.info("Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
        waitNotifyEngine.notify(context.getCorrelationId(), response);
      }
    }
  }
}
