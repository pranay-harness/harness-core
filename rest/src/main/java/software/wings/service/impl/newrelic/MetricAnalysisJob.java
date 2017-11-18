package software.wings.service.impl.newrelic;

import com.google.common.base.Preconditions;

import org.apache.commons.lang.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.delegatetasks.NewRelicDataCollectionTask;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.Threshold;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
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
import javax.inject.Inject;

/**
 * Created by rsingh on 9/11/17.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class MetricAnalysisJob implements Job {
  @Inject private MetricDataAnalysisService analysisService;

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
      new MetricAnalysisGenerator(context, jobExecutionContext, delegateTaskId).run();

    } catch (Exception ex) {
      logger.warn("Log analysis cron failed with error", ex);
      try {
        jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
      } catch (SchedulerException e) {
        logger.error("Unable to clean up cron", e);
      }
    }
  }

  public class MetricAnalysisGenerator implements Runnable {
    public static final int PYTHON_JOB_RETRIES = 3;
    public static final String LOG_ML_ROOT = "SPLUNKML_ROOT";
    protected static final String TS_ML_SHELL_FILE_NAME = "run_time_series_ml.sh";
    private final String pythonScriptRoot;

    private final AnalysisContext context;
    private final JobExecutionContext jobExecutionContext;
    private final String delegateTaskId;
    private final Set<String> testNodes;
    private final Set<String> controlNodes;

    public MetricAnalysisGenerator(
        AnalysisContext context, JobExecutionContext jobExecutionContext, String delegateTaskId) {
      this.pythonScriptRoot = System.getenv(LOG_ML_ROOT);
      Preconditions.checkState(!StringUtils.isBlank(pythonScriptRoot), "SPLUNKML_ROOT can not be null or empty");
      this.context = context;
      this.jobExecutionContext = jobExecutionContext;
      this.delegateTaskId = delegateTaskId;
      this.testNodes = context.getTestNodes();
      this.controlNodes = context.getControlNodes();
      if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        this.controlNodes.removeAll(this.testNodes);
      }
    }

    private NewRelicMetricAnalysisRecord analyzeLocal(int analysisMinute) {
      logger.info("running " + context.getStateType().name() + " for minute {}", analysisMinute);
      final List<NewRelicMetricDataRecord> controlRecords =
          context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS
          ? analysisService.getPreviousSuccessfulRecords(
                context.getStateType(), context.getWorkflowId(), context.getServiceId(), analysisMinute)
          : analysisService.getRecords(context.getStateType(), context.getWorkflowExecutionId(),
                context.getStateExecutionId(), context.getWorkflowId(), context.getServiceId(),
                context.getControlNodes(), analysisMinute);

      final List<NewRelicMetricDataRecord> testRecords = analysisService.getRecords(context.getStateType(),
          context.getWorkflowExecutionId(), context.getStateExecutionId(), context.getWorkflowId(),
          context.getServiceId(), context.getTestNodes(), analysisMinute);

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

      Map<String, List<Threshold>> stateValuesToAnalyze;
      switch (context.getStateType()) {
        case NEW_RELIC:
          stateValuesToAnalyze = NewRelicMetricValueDefinition.NEW_RELIC_VALUES_TO_ANALYZE;
          break;
        case APP_DYNAMICS:
          stateValuesToAnalyze = NewRelicMetricValueDefinition.APP_DYNAMICS_VALUES_TO_ANALYZE;
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

        for (Entry<String, List<Threshold>> valuesToAnalyze : stateValuesToAnalyze.entrySet()) {
          NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                    .metricName(metricName)
                                                                    .metricValueName(valuesToAnalyze.getKey())
                                                                    .thresholds(valuesToAnalyze.getValue())
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

    private void timeSeriesML(int analysisMinute) throws InterruptedException, TimeoutException, IOException {
      String protocol = context.isSSL() ? "https" : "http";
      String serverUrl = protocol + "://localhost:" + context.getAppPort();

      String testInputUrl = serverUrl + "/api/" + context.getStateBaseUrl()
          + "/get-metrics?accountId=" + context.getAccountId()
          + "&workflowExecutionId=" + context.getWorkflowExecutionId() + "&compareCurrent=true";
      String controlInputUrl = serverUrl + "/api/" + context.getStateBaseUrl()
          + "/get-metrics?accountId=" + context.getAccountId() + "&compareCurrent=";
      if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        controlInputUrl = controlInputUrl + true + "&workflowExecutionId=" + context.getWorkflowExecutionId();
      } else {
        controlInputUrl = controlInputUrl + false + "&workflowExecutionId=" + context.getPrevWorkflowExecutionId();
      }

      final String logAnalysisSaveUrl = serverUrl + "/api/" + context.getStateBaseUrl()
          + "/save-analysis?accountId=" + context.getAccountId() + "&applicationId=" + context.getAppId() + "&"
          + "workflowExecutionId=" + context.getWorkflowExecutionId()
          + "&stateExecutionId=" + context.getStateExecutionId() + "&analysisMinute=" + analysisMinute;

      final List<String> command = new ArrayList<>();
      command.add(this.pythonScriptRoot + "/" + TS_ML_SHELL_FILE_NAME);

      command.add("--control_input_url");
      command.add(controlInputUrl);
      command.add("--test_input_url");
      command.add(testInputUrl);
      command.add("--control_nodes");
      command.addAll(controlNodes);
      command.add("--test_nodes");
      command.addAll(testNodes);
      command.add("--auth_token=" + context.getAuthToken());
      command.add("--application_id=" + context.getAppId());
      command.add("--workflow_id=" + context.getWorkflowId());
      command.add("--workflow_execution_id=" + context.getWorkflowExecutionId());
      command.add("--service_id=" + context.getServiceId());
      command.add("--analysis_minute");
      command.add(String.valueOf(analysisMinute));
      command.add("--state_execution_id=" + context.getStateExecutionId());
      command.add("--analysis_save_url");
      command.add(logAnalysisSaveUrl);
      command.add("--smooth_window");
      command.add(String.valueOf(context.getSmooth_window()));
      command.add("--tolerance");
      command.add(String.valueOf(context.getTolerance()));
      command.add("--min_rpm");
      command.add(String.valueOf(context.getMinimumRequestsPerMinute()));

      int attempt = 0;
      for (; attempt < PYTHON_JOB_RETRIES; attempt++) {
        final ProcessResult result =
            new ProcessExecutor(command)
                .redirectOutput(
                    Slf4jStream.of(LoggerFactory.getLogger(getClass().getName() + "." + context.getStateExecutionId()))
                        .asInfo())
                .execute();

        switch (result.getExitValue()) {
          case 0:
            logger.info("Metric analysis done for " + context.getStateExecutionId() + " for minute " + analysisMinute);
            attempt += PYTHON_JOB_RETRIES;
            break;
          case 2:
            logger.warn("No test data from the deployed nodes " + context.getStateExecutionId() + " for minute "
                + analysisMinute);
            attempt += PYTHON_JOB_RETRIES;
            break;
          default:
            logger.warn("Log analysis failed for " + context.getStateExecutionId() + " for minute " + analysisMinute
                + " trial: " + (attempt + 1));
            Thread.sleep(2000);
        }
      }

      if (attempt == PYTHON_JOB_RETRIES) {
        throw new RuntimeException("Finished all retries.");
      }
    }

    @Override
    public void run() {
      logger.info("Starting analysis for " + context.getStateExecutionId());

      boolean completeCron = false;

      try {
        /**
         * Work flow is invalid
         * exit immediately
         **/
        if (!analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
          completeCron = true;
          return;
        }

        final int analysisMinute = analysisService.getCollectionMinuteToProcess(context.getStateType(),
            context.getStateExecutionId(), context.getWorkflowExecutionId(), context.getServiceId());

        logger.info("running analysis for " + context.getStateExecutionId() + " for minute" + analysisMinute);

        if (analysisMinute > context.getTimeDuration() - 1) {
          logger.info("time series analysis finished after running for {} minutes", analysisMinute);
          completeCron = true;
          return;
        }

        int maxControlMinute = Integer.MAX_VALUE;
        if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS) {
          maxControlMinute = analysisService.getMaxControlMinute(context.getStateType(), context.getServiceId(),
              context.getWorkflowId(), context.getPrevWorkflowExecutionId());
        }

        boolean isBaseLineCreated =
            context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT || maxControlMinute > -1;
        if (isBaseLineCreated && context.getStateType() == StateType.NEW_RELIC) {
          if (analysisMinute <= maxControlMinute) {
            timeSeriesML(analysisMinute);
          } else {
            logger.warn("Not enough control data. analysis minute = " + analysisMinute
                + " , max control minute = " + maxControlMinute);
          }
        } else {
          NewRelicMetricAnalysisRecord analysisRecord = analyzeLocal(analysisMinute);
          analysisService.saveAnalysisRecords(analysisRecord);
        }
        analysisService.bumpCollectionMinuteToProcess(context.getStateType(), context.getStateExecutionId(),
            context.getWorkflowExecutionId(), context.getServiceId(), analysisMinute);

        int nextAnalysisMinute = analysisService.getCollectionMinuteToProcess(context.getStateType(),
            context.getStateExecutionId(), context.getWorkflowExecutionId(), context.getServiceId());

        logger.info("Finish analysis for " + context.getStateExecutionId() + " for minute" + analysisMinute
            + ". Next minute is " + nextAnalysisMinute);
      } catch (Exception ex) {
        completeCron = true;
        logger.warn("analysis failed", ex);
      } finally {
        try {
          // send notification to state manager and delete cron.
          if (completeCron || !analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            try {
              delegateService.abortTask(context.getAccountId(), delegateTaskId);
              sendStateNotification(context);
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

    private void sendStateNotification(AnalysisContext context) {
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
      executionData.setStatus(ExecutionStatus.SUCCESS);
      final MetricDataAnalysisResponse response =
          MetricDataAnalysisResponse.builder().stateExecutionData(executionData).build();
      response.setExecutionStatus(ExecutionStatus.SUCCESS);
      waitNotifyEngine.notify(context.getCorrelationId(), response);
    }
  }
}
