package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.common.VerificationConstants.VERIFICATION_SERVICE_BASE_URL;
import static software.wings.common.VerificationConstants.getMetricAnalysisStates;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.analysis.TimeSeriesMlAnalysisType.PREDICTIVE;
import static software.wings.sm.states.AbstractMetricAnalysisState.COMPARISON_WINDOW;
import static software.wings.sm.states.AbstractMetricAnalysisState.MIN_REQUESTS_PER_MINUTE;
import static software.wings.sm.states.AbstractMetricAnalysisState.PARALLEL_PROCESSES;
import static software.wings.sm.states.AbstractMetricAnalysisState.SMOOTH_WINDOW;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
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

/**
 * Created by rsingh on 10/9/18.
 */
public class ContinuousVerificationServiceImpl implements ContinuousVerificationService {
  private static final Logger logger = LoggerFactory.getLogger(ContinuousVerificationServiceImpl.class);

  // todo: need to change this. Group name is set to default.
  private final String DEFAULT_GROUP_NAME = "default";

  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private LearningEngineService learningEngineService;
  @Inject private TimeSeriesAnalysisService analysisService;

  @Override
  public boolean triggerDataCollection(String accountId) {
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);
    long endMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    cvConfigurations.stream().filter(cvConfiguration -> cvConfiguration.isEnabled24x7()).forEach(cvConfiguration -> {
      long maxCVCollectionMinute =
          timeSeriesAnalysisService.getMaxCVCollectionMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
      long startTime = maxCVCollectionMinute <= 0 || endMinute - maxCVCollectionMinute > PREDECTIVE_HISTORY_MINUTES
          ? TimeUnit.MINUTES.toMillis(endMinute) - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES)
              - TimeUnit.SECONDS.toMillis(CRON_POLL_INTERVAL)
          : TimeUnit.MINUTES.toMillis(maxCVCollectionMinute + 1);
      long endTime = TimeUnit.MINUTES.toMillis(endMinute);
      if (getMetricAnalysisStates().contains(cvConfiguration.getStateType())) {
        logger.info("triggering data collection for state {} config {} startTime {} endTime {} collectionMinute {}",
            cvConfiguration.getStateType(), cvConfiguration.getUuid(), startTime, endMinute, endMinute);
        verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerAPMDataCollection(
            cvConfiguration.getUuid(), cvConfiguration.getStateType(), startTime, endTime));
      }
    });
    return true;
  }

  @Override
  public void triggerDataAnalysis(String accountId) {
    logger.info("Triggering Data Analysis for account {} ", accountId);
    // List all the CV configurations for a given account
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);

    // for each 24x7 enabled configurations do following
    // Get last CV Analysis minute on given configuration
    // Add new learning task for next ANALYSIS_PERIOD_MINUTES period
    cvConfigurations.stream().filter(cvConfiguration -> cvConfiguration.isEnabled24x7()).forEach(cvConfiguration -> {
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

      if (getMetricAnalysisStates().contains(cvConfiguration.getStateType())) {
        while (lastCVDataCollectionMinute - lastCVAnalysisMinute >= TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL)) {
          long analysisStartMinute = lastCVAnalysisMinute <= 0
              ? lastCVDataCollectionMinute - TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL)
              : lastCVAnalysisMinute;
          long endMinute = analysisStartMinute + TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL);

          LearningEngineAnalysisTask learningEngineAnalysisTask =
              createLearningEngineAnalysisTask(accountId, cvConfiguration, analysisStartMinute, endMinute);

          learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);

          logger.info("Queuing analysis task for state {} config {} with startTime {}", cvConfiguration.getStateType(),
              cvConfiguration.getUuid(), analysisStartMinute);

          if (lastCVAnalysisMinute <= 0) {
            lastCVAnalysisMinute = endMinute;
          } else {
            lastCVAnalysisMinute += TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL);
          }
        }
      }
    });
  }

  private LearningEngineAnalysisTask createLearningEngineAnalysisTask(
      String accountId, CVConfiguration cvConfiguration, long startMin, long endMin) {
    String learningTaskId = generateUuid();

    String testInputUrl = getDataFetchUrl(cvConfiguration, startMin - PREDECTIVE_HISTORY_MINUTES, endMin);
    String metricAnalysisSaveUrl = getMetricAnalysisSaveUrl(cvConfiguration, endMin, learningTaskId);

    String metricTemplateUrl = getMetricTemplateUrl(accountId, cvConfiguration.getAppId(),
        cvConfiguration.getStateType(), cvConfiguration.getServiceId(), cvConfiguration.getUuid());

    LearningEngineAnalysisTask learningEngineAnalysisTask =
        LearningEngineAnalysisTask.builder()
            .service_id(cvConfiguration.getServiceId())
            .state_execution_id(CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid() + "-" + generateUuid())
            .analysis_start_min((int) startMin - PREDECTIVE_HISTORY_MINUTES)
            .analysis_minute((int) endMin)
            .analysis_start_time((int) startMin)
            .smooth_window(0)
            .tolerance(0)
            .min_rpm(0)
            .comparison_unit_window(0)
            .parallel_processes(0)
            .test_input_url(testInputUrl)
            .previous_analysis_url(getPreviousAnalysisUrl(cvConfiguration))
            .control_input_url("")
            .analysis_save_url(metricAnalysisSaveUrl)
            .metric_template_url(metricTemplateUrl)
            .control_nodes(Sets.newHashSet("dummy"))
            .test_nodes(Sets.newHashSet("dummy"))
            .stateType(cvConfiguration.getStateType())
            .ml_analysis_type(MLAnalysisType.TIME_SERIES)
            .time_series_ml_analysis_type(PREDICTIVE)
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
}
