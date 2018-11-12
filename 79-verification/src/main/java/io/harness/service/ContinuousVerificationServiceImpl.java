package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.common.VerificationConstants.DATA_ANALYSIS_TASKS_PER_MINUTE;
import static software.wings.common.VerificationConstants.DATA_COLLECTION_TASKS_PER_MINUTE;
import static software.wings.common.VerificationConstants.TIME_DELAY_QUERY_MINS;
import static software.wings.common.VerificationConstants.VERIFICATION_SERVICE_BASE_URL;
import static software.wings.common.VerificationConstants.getMetricAnalysisStates;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.analysis.TimeSeriesMlAnalysisType.TIMESERIES_24x7;
import static software.wings.sm.states.AbstractMetricAnalysisState.COMPARISON_WINDOW;
import static software.wings.sm.states.AbstractMetricAnalysisState.MIN_REQUESTS_PER_MINUTE;
import static software.wings.sm.states.AbstractMetricAnalysisState.PARALLEL_PROCESSES;
import static software.wings.sm.states.AbstractMetricAnalysisState.SMOOTH_WINDOW;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.Counted;
import com.codahale.metrics.annotation.Timed;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.registry.HarnessMetricRegistry;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rsingh on 10/9/18.
 */
public class ContinuousVerificationServiceImpl implements ContinuousVerificationService {
  private static final Logger logger = LoggerFactory.getLogger(ContinuousVerificationServiceImpl.class);

  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private LearningEngineService learningEngineService;
  @Inject private TimeSeriesAnalysisService analysisService;
  @Inject private HarnessMetricRegistry metricRegistry;

  @Override
  @Counted
  @Timed
  public boolean triggerDataCollection(String accountId) {
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);
    long endMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - TIME_DELAY_QUERY_MINS;
    AtomicLong totalDataCollectionTasks = new AtomicLong(0);
    cvConfigurations.stream().filter(cvConfiguration -> cvConfiguration.isEnabled24x7()).forEach(cvConfiguration -> {
      long maxCVCollectionMinute =
          timeSeriesAnalysisService.getMaxCVCollectionMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
      long startTime = maxCVCollectionMinute <= 0 || endMinute - maxCVCollectionMinute > PREDECTIVE_HISTORY_MINUTES
          ? TimeUnit.MINUTES.toMillis(endMinute) - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES)
              - TimeUnit.SECONDS.toMillis(CRON_POLL_INTERVAL)
          : TimeUnit.MINUTES.toMillis(maxCVCollectionMinute);
      long endTime = TimeUnit.MINUTES.toMillis(endMinute);
      if (getMetricAnalysisStates().contains(cvConfiguration.getStateType())) {
        logger.info("triggering data collection for state {} config {} startTime {} endTime {} collectionMinute {}",
            cvConfiguration.getStateType(), cvConfiguration.getUuid(), startTime, endMinute, endMinute);
        verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerAPMDataCollection(
            cvConfiguration.getUuid(), cvConfiguration.getStateType(), startTime, endTime));
        totalDataCollectionTasks.getAndIncrement();
      }
    });
    metricRegistry.updateMetricValue(DATA_COLLECTION_TASKS_PER_MINUTE, totalDataCollectionTasks.get());
    return true;
  }

  @Override
  @Counted
  @Timed
  public void triggerDataAnalysis(String accountId) {
    logger.info("Triggering Data Analysis for account {} ", accountId);
    // List all the CV configurations for a given account
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);

    AtomicLong totalDataAnalysisTasks = new AtomicLong(0);

    // for each 24x7 enabled configurations do following
    // Get last CV Analysis minute on given configuration
    // Add new learning task for next ANALYSIS_PERIOD_MINUTES period
    cvConfigurations.stream().filter(cvConfiguration -> cvConfiguration.isEnabled24x7()).forEach(cvConfiguration -> {
      if (!getMetricAnalysisStates().contains(cvConfiguration.getStateType())) {
        return;
      }

      long lastCVDataCollectionMinute =
          timeSeriesAnalysisService.getMaxCVCollectionMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
      if (lastCVDataCollectionMinute <= 0) {
        logger.info("For account {} and CV config {} name {} type {} no data has been collected yet. Skipping analysis",
            cvConfiguration.getAccountId(), cvConfiguration.getUuid(), cvConfiguration.getName(),
            cvConfiguration.getStateType());
        return;
      }

      long lastCVAnalysisMinute =
          timeSeriesAnalysisService.getLastCVAnalysisMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());

      if (lastCVAnalysisMinute <= 0) {
        logger.info(
            "For account {} and CV config {} name {} type {} no analysis has been done yet. This is going to be first analysis",
            cvConfiguration.getAccountId(), cvConfiguration.getUuid(), cvConfiguration.getName(),
            cvConfiguration.getStateType());
      }

      if (lastCVDataCollectionMinute - lastCVAnalysisMinute >= TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL)) {
        long analysisStartMinute = lastCVAnalysisMinute <= 0
            ? lastCVDataCollectionMinute - TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL)
            : lastCVAnalysisMinute;
        long endMinute = analysisStartMinute + TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL);

        // since analysis startMin is inclusive in LE, we  need to add 1.
        analysisStartMinute += 1;

        LearningEngineAnalysisTask learningEngineAnalysisTask =
            createLearningEngineAnalysisTask(accountId, cvConfiguration, analysisStartMinute, endMinute);

        learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);

        totalDataAnalysisTasks.getAndIncrement();
        logger.info("Queuing analysis task for state {} config {} with startTime {}", cvConfiguration.getStateType(),
            cvConfiguration.getUuid(), analysisStartMinute);
      }
    });
    metricRegistry.updateMetricValue(DATA_ANALYSIS_TASKS_PER_MINUTE, totalDataAnalysisTasks.get());
  }

  private LearningEngineAnalysisTask createLearningEngineAnalysisTask(
      String accountId, CVConfiguration cvConfiguration, long startMin, long endMin) {
    String learningTaskId = generateUuid();

    String testInputUrl = getDataFetchUrl(cvConfiguration, startMin - PREDECTIVE_HISTORY_MINUTES, endMin);
    String metricAnalysisSaveUrl = getMetricAnalysisSaveUrl(cvConfiguration, endMin, learningTaskId);
    String historicalAnalysisUrl = getHistoricalAnalysisUrl(cvConfiguration, endMin);

    String metricTemplateUrl = getMetricTemplateUrl(accountId, cvConfiguration.getAppId(),
        cvConfiguration.getStateType(), cvConfiguration.getServiceId(), cvConfiguration.getUuid());

    final String stateExecutionIdForLETask = CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid() + "-" + endMin;

    // clear up any old failed task with the same ID and time.
    learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) endMin);

    LearningEngineAnalysisTask learningEngineAnalysisTask =
        LearningEngineAnalysisTask.builder()
            .service_id(cvConfiguration.getServiceId())
            .state_execution_id(stateExecutionIdForLETask)
            .cvConfigId(cvConfiguration.getUuid())
            .analysis_start_min((int) startMin - PREDECTIVE_HISTORY_MINUTES)
            .analysis_minute((int) endMin)
            .prediction_start_time((int) startMin - 1)
            .smooth_window(0)
            .tolerance(0)
            .min_rpm(0)
            .comparison_unit_window(0)
            .parallel_processes(0)
            .test_input_url(testInputUrl)
            .previous_analysis_url(getPreviousAnalysisUrl(cvConfiguration))
            .historical_analysis_url(historicalAnalysisUrl)
            .control_input_url("")
            .analysis_save_url(metricAnalysisSaveUrl)
            .metric_template_url(metricTemplateUrl)
            .control_nodes(Sets.newHashSet("dummy"))
            .test_nodes(Sets.newHashSet("dummy"))
            .stateType(cvConfiguration.getStateType())
            .ml_analysis_type(MLAnalysisType.TIME_SERIES)
            .time_series_ml_analysis_type(TIMESERIES_24x7)
            .smooth_window(SMOOTH_WINDOW)
            .tolerance(cvConfiguration.getAnalysisTolerance().tolerance())
            .min_rpm(MIN_REQUESTS_PER_MINUTE)
            .comparison_unit_window(COMPARISON_WINDOW)
            .parallel_processes(PARALLEL_PROCESSES)
            .build();
    learningEngineAnalysisTask.setAppId(cvConfiguration.getAppId());
    learningEngineAnalysisTask.setUuid(learningTaskId);

    return learningEngineAnalysisTask;
  }

  public String getMetricTemplateUrl(
      String accountId, String appId, StateType stateType, String serviceId, String cvConfigId) {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/get-metric-template_24_7?accountId=" + accountId + "&appId=" + appId + "&stateType=" + stateType
        + "&serviceId=" + serviceId + "&cvConfigId=" + cvConfigId;
  }

  private String getMetricAnalysisSaveUrl(CVConfiguration cvConfiguration, long endMinute, String taskId) {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/save-analysis"
        + "?accountId=" + cvConfiguration.getAccountId() + "&applicationId=" + cvConfiguration.getAppId()
        + "&stateType=" + cvConfiguration.getStateType() + "&serviceId=" + cvConfiguration.getServiceId()
        + "&analysisMinute=" + endMinute + "&cvConfigId=" + cvConfiguration.getUuid() + "&taskId=" + taskId;
  }

  private String getDataFetchUrl(CVConfiguration cvConfiguration, long startMinute, long endMinute) {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/get-metric-data-247?accountId=" + cvConfiguration.getAccountId() + "&appId=" + cvConfiguration.getAppId()
        + "&stateType=" + cvConfiguration.getStateType() + "&cvConfigId=" + cvConfiguration.getUuid() + "&serviceId="
        + cvConfiguration.getServiceId() + "&analysisStartMin=" + startMinute + "&analysisEndMin=" + endMinute;
  }

  private String getPreviousAnalysisUrl(CVConfiguration cvConfiguration) {
    long min = timeSeriesAnalysisService.getLastCVAnalysisMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/previous-analysis-247?appId=" + cvConfiguration.getAppId() + "&cvConfigId=" + cvConfiguration.getUuid()
        + "&dataCollectionMin=" + min;
  }

  private String getHistoricalAnalysisUrl(CVConfiguration cvConfiguration, long minute) {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/historical-analysis-24x7?accountId=" + cvConfiguration.getAccountId()
        + "&applicationId=" + cvConfiguration.getAppId() + "&serviceId=" + cvConfiguration.getServiceId()
        + "&analysisMinute=" + minute + "&cvConfigId=" + cvConfiguration.getUuid();
  }
}
