package io.harness.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.UuidAccess.ID_KEY;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.common.VerificationConstants.DATA_COLLECTION_TASKS_PER_MINUTE;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.common.VerificationConstants.TIME_DELAY_QUERY_MINS;
import static software.wings.common.VerificationConstants.VERIFICATION_SERVICE_BASE_URL;
import static software.wings.common.VerificationConstants.getLogAnalysisStates;
import static software.wings.common.VerificationConstants.getMetricAnalysisStates;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.PREDICTIVE;
import static software.wings.service.impl.analysis.LogAnalysisResponse.Builder.aLogAnalysisResponse;
import static software.wings.service.impl.analysis.TimeSeriesMlAnalysisType.TIMESERIES_24x7;
import static software.wings.sm.states.AbstractMetricAnalysisState.COMPARISON_WINDOW;
import static software.wings.sm.states.AbstractMetricAnalysisState.MIN_REQUESTS_PER_MINUTE;
import static software.wings.sm.states.AbstractMetricAnalysisState.PARALLEL_PROCESSES;
import static software.wings.sm.states.AbstractMetricAnalysisState.SMOOTH_WINDOW;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.Counted;
import com.codahale.metrics.annotation.Timed;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SortOrder.OrderType;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.persistence.ReadPref;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogAnalysisExecutionData;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
  @Inject private LogAnalysisService logAnalysisService;
  @Inject private HarnessMetricRegistry metricRegistry;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UsageMetricsHelper usageMetricsHelper;

  @Override
  @Counted
  @Timed
  public boolean triggerAPMDataCollection(String accountId) {
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);
    long endMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - TIME_DELAY_QUERY_MINS;
    AtomicLong totalDataCollectionTasks = new AtomicLong(0);
    cvConfigurations.stream()
        .filter(cvConfiguration
            -> cvConfiguration.isEnabled24x7() && getMetricAnalysisStates().contains(cvConfiguration.getStateType()))
        .forEach(cvConfiguration -> {
          long maxCVCollectionMinute =
              timeSeriesAnalysisService.getMaxCVCollectionMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
          long startTime = maxCVCollectionMinute <= 0 || endMinute - maxCVCollectionMinute > PREDECTIVE_HISTORY_MINUTES
              ? TimeUnit.MINUTES.toMillis(endMinute) - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES)
                  - TimeUnit.SECONDS.toMillis(CRON_POLL_INTERVAL)
              : TimeUnit.MINUTES.toMillis(maxCVCollectionMinute);
          long endTime = TimeUnit.MINUTES.toMillis(endMinute);
          if (endTime - startTime >= TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES / 3)) {
            logger.info("triggering data collection for state {} config {} startTime {} endTime {} collectionMinute {}",
                cvConfiguration.getStateType(), cvConfiguration.getUuid(), startTime, endTime, endMinute);
            verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerCVDataCollection(
                cvConfiguration.getUuid(), cvConfiguration.getStateType(), startTime, endTime));
            totalDataCollectionTasks.getAndIncrement();
          }
        });
    metricRegistry.recordGaugeValue(DATA_COLLECTION_TASKS_PER_MINUTE, null, totalDataCollectionTasks.get());
    return true;
  }

  @Override
  @Counted
  @Timed
  public void triggerMetricDataAnalysis(String accountId) {
    logger.info("Triggering Data Analysis for account {} ", accountId);
    // List all the CV configurations for a given account
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);

    // for each 24x7 enabled configurations do following
    // Get last CV Analysis minute on given configuration
    // Add new learning task for next ANALYSIS_PERIOD_MINUTES period
    cvConfigurations.stream()
        .filter(cvConfiguration
            -> cvConfiguration.isEnabled24x7() && getMetricAnalysisStates().contains(cvConfiguration.getStateType()))
        .forEach(cvConfiguration -> {
          try {
            logger.info("Executing APM data analysis Job for accountId {} and configId {}", accountId,
                cvConfiguration.getUuid());
            long lastCVDataCollectionMinute = timeSeriesAnalysisService.getMaxCVCollectionMinute(
                cvConfiguration.getAppId(), cvConfiguration.getUuid());
            if (lastCVDataCollectionMinute <= 0) {
              logger.info(
                  "For account {} and CV config {} name {} type {} no data has been collected yet. Skipping analysis",
                  cvConfiguration.getAccountId(), cvConfiguration.getUuid(), cvConfiguration.getName(),
                  cvConfiguration.getStateType());
              return;
            }

            long lastCVAnalysisMinute = timeSeriesAnalysisService.getLastCVAnalysisMinute(
                cvConfiguration.getAppId(), cvConfiguration.getUuid());

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

              Set<String> tags = getTags(cvConfiguration.getUuid(), cvConfiguration.getAppId());
              List<MLExperiments> experiments = get24x7Experiments();
              if (isEmpty(tags)) {
                LearningEngineAnalysisTask learningEngineAnalysisTask =
                    createLearningEngineAnalysisTask(accountId, cvConfiguration, analysisStartMinute, endMinute, null);

                learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);

                logger.info("Triggering Data Analysis for account {} ", accountId);
                logger.info("Queuing analysis task for state {} config {} and tag {} with startTime {}",
                    cvConfiguration.getStateType(), cvConfiguration.getUuid(), null, analysisStartMinute);

                for (MLExperiments experiment : experiments) {
                  LearningEngineExperimentalAnalysisTask task = createLearningEngineAnalysisExperimentalTask(
                      accountId, cvConfiguration, analysisStartMinute, endMinute, null, experiment.getExperimentName());
                  learningEngineService.addLearningEngineExperimentalAnalysisTask(task);
                }
              }

              for (String tag : tags) {
                LearningEngineAnalysisTask learningEngineAnalysisTask =
                    createLearningEngineAnalysisTask(accountId, cvConfiguration, analysisStartMinute, endMinute, tag);

                learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);

                logger.info("Triggering Data Analysis for account {} ", accountId);
                logger.info("Queuing analysis task for state {} config {} and tag {} with startTime {}",
                    cvConfiguration.getStateType(), cvConfiguration.getUuid(), tag, analysisStartMinute);

                for (MLExperiments experiment : experiments) {
                  LearningEngineExperimentalAnalysisTask task = createLearningEngineAnalysisExperimentalTask(
                      accountId, cvConfiguration, analysisStartMinute, endMinute, null, experiment.getExperimentName());
                  learningEngineService.addLearningEngineExperimentalAnalysisTask(task);
                  logger.info(
                      "Created experimental analysis task for state {} config {} and tag {} with startTime {}, experimentName {}",
                      cvConfiguration.getStateType(), cvConfiguration.getUuid(), tag, analysisStartMinute,
                      experiment.getExperimentName());
                }
              }
            }
          } catch (Exception ex) {
            logger.error("Exception occurred while triggering metric data collection for cvConfig {}",
                cvConfiguration.getUuid(), ex);
          }
        });
  }

  private Set<String> getTags(String cvConfigId, String appId) {
    Set<String> tags = new HashSet<>();
    TimeSeriesMetricTemplates template = wingsPersistence.createQuery(TimeSeriesMetricTemplates.class)
                                             .filter("appId", appId)
                                             .filter("cvConfigId", cvConfigId)
                                             .get();

    if (template != null) {
      template.getMetricTemplates().forEach((key, value) -> {
        if (isNotEmpty(value.getTags())) {
          tags.addAll(value.getTags());
        }
      });
    }
    return tags;
  }

  private List<MLExperiments> get24x7Experiments() {
    return wingsPersistence.createQuery(MLExperiments.class, excludeAuthority).filter("is24x7", true).asList();
  }

  private LearningEngineAnalysisTask createLearningEngineAnalysisTask(
      String accountId, CVConfiguration cvConfiguration, long startMin, long endMin, String tag) {
    String learningTaskId = generateUuid();

    String testInputUrl = getDataFetchUrl(cvConfiguration, startMin - PREDECTIVE_HISTORY_MINUTES, endMin, tag);
    String metricAnalysisSaveUrl = getMetricAnalysisSaveUrl(cvConfiguration, endMin, learningTaskId, tag);
    String historicalAnalysisUrl = getHistoricalAnalysisUrl(cvConfiguration, endMin, tag);

    String metricTemplateUrl = getMetricTemplateUrl(accountId, cvConfiguration.getAppId(),
        cvConfiguration.getStateType(), cvConfiguration.getServiceId(), cvConfiguration.getUuid());

    final String stateExecutionIdForLETask = CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid();

    // clear up any old failed task with the same ID and time.
    learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) endMin);

    LearningEngineAnalysisTask learningEngineAnalysisTask =
        LearningEngineAnalysisTask.builder()
            .service_id(cvConfiguration.getServiceId())
            .state_execution_id(stateExecutionIdForLETask)
            .cvConfigId(cvConfiguration.getUuid())
            .analysis_start_min((int) startMin - PREDECTIVE_HISTORY_MINUTES)
            .analysis_minute(endMin)
            .prediction_start_time((int) startMin - 1)
            .smooth_window(0)
            .tolerance(0)
            .min_rpm(0)
            .comparison_unit_window(0)
            .parallel_processes(0)
            .test_input_url(testInputUrl)
            .previous_analysis_url(getPreviousAnalysisUrl(cvConfiguration, tag))
            .historical_analysis_url(historicalAnalysisUrl)
            .control_input_url("")
            .analysis_save_url(metricAnalysisSaveUrl)
            .metric_template_url(metricTemplateUrl)
            .previous_anomalies_url(getPreviousAnomaliesUrl(cvConfiguration, tag))
            .cumulative_sums_url(getCumulativeSumsUrl(cvConfiguration, (int) endMin, tag))
            .control_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
            .test_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
            .stateType(cvConfiguration.getStateType())
            .ml_analysis_type(MLAnalysisType.TIME_SERIES)
            .time_series_ml_analysis_type(TIMESERIES_24x7)
            .smooth_window(SMOOTH_WINDOW)
            .tolerance(cvConfiguration.getAnalysisTolerance().tolerance())
            .min_rpm(MIN_REQUESTS_PER_MINUTE)
            .comparison_unit_window(COMPARISON_WINDOW)
            .parallel_processes(PARALLEL_PROCESSES)
            .is24x7Task(true)
            .tag(tag)
            .build();
    learningEngineAnalysisTask.setAppId(cvConfiguration.getAppId());
    learningEngineAnalysisTask.setUuid(learningTaskId);

    return learningEngineAnalysisTask;
  }

  private LearningEngineExperimentalAnalysisTask createLearningEngineAnalysisExperimentalTask(String accountId,
      CVConfiguration cvConfiguration, long startMin, long endMin, String tag, String experimentName) {
    String learningTaskId = generateUuid();

    String testInputUrl = getDataFetchUrl(cvConfiguration, startMin - PREDECTIVE_HISTORY_MINUTES, endMin, tag);
    String historicalAnalysisUrl = getHistoricalAnalysisUrl(cvConfiguration, endMin, tag);

    String metricTemplateUrl = getMetricTemplateUrl(accountId, cvConfiguration.getAppId(),
        cvConfiguration.getStateType(), cvConfiguration.getServiceId(), cvConfiguration.getUuid());

    final String stateExecutionIdForLETask = CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid() + "-" + tag;

    LearningEngineExperimentalAnalysisTask learningEngineAnalysisTask =
        LearningEngineExperimentalAnalysisTask.builder()
            .service_id(cvConfiguration.getServiceId())
            .state_execution_id(stateExecutionIdForLETask)
            .cvConfigId(cvConfiguration.getUuid())
            .analysis_start_min((int) startMin - PREDECTIVE_HISTORY_MINUTES)
            .analysis_minute(endMin)
            .prediction_start_time((int) startMin - 1)
            .smooth_window(0)
            .tolerance(0)
            .min_rpm(0)
            .comparison_unit_window(0)
            .parallel_processes(0)
            .test_input_url(testInputUrl)
            .previous_analysis_url(getPreviousAnalysisUrl(cvConfiguration, tag))
            .historical_analysis_url(historicalAnalysisUrl)
            .control_input_url("")
            .analysis_save_url(getSaveUrlForExperimentalTask())
            .metric_template_url(metricTemplateUrl)
            .previous_anomalies_url(getPreviousAnomaliesUrl(cvConfiguration, tag))
            .cumulative_sums_url(getCumulativeSumsUrl(cvConfiguration, (int) endMin, tag))
            .control_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
            .test_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
            .stateType(cvConfiguration.getStateType())
            .ml_analysis_type(MLAnalysisType.TIME_SERIES)
            .time_series_ml_analysis_type(TIMESERIES_24x7)
            .smooth_window(SMOOTH_WINDOW)
            .tolerance(cvConfiguration.getAnalysisTolerance().tolerance())
            .min_rpm(MIN_REQUESTS_PER_MINUTE)
            .comparison_unit_window(COMPARISON_WINDOW)
            .parallel_processes(PARALLEL_PROCESSES)
            .is24x7Task(true)
            .tag(tag)
            .experiment_name(experimentName)
            .build();
    learningEngineAnalysisTask.setAppId(cvConfiguration.getAppId());
    learningEngineAnalysisTask.setUuid(learningTaskId);

    return learningEngineAnalysisTask;
  }

  private String getSaveUrlForExperimentalTask() {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/save-dummy-experimental-247?cvConfigId=Dummy";
  }

  private String getMetricTemplateUrl(
      String accountId, String appId, StateType stateType, String serviceId, String cvConfigId) {
    final String stateExecutionIdForLETask = CV_24x7_STATE_EXECUTION + "-" + cvConfigId;
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/get-metric-template?accountId=" + accountId + "&appId=" + appId + "&stateType=" + stateType
        + "&serviceId=" + serviceId + "&cvConfigId=" + cvConfigId + "&stateExecutionId=" + stateExecutionIdForLETask;
  }

  private String getMetricAnalysisSaveUrl(CVConfiguration cvConfiguration, long endMinute, String taskId, String tag) {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/save-analysis"
        + "?accountId=" + cvConfiguration.getAccountId() + "&applicationId=" + cvConfiguration.getAppId()
        + "&stateType=" + cvConfiguration.getStateType() + "&serviceId=" + cvConfiguration.getServiceId()
        + "&analysisMinute=" + endMinute + "&cvConfigId=" + cvConfiguration.getUuid() + "&taskId=" + taskId
        + (tag != null ? ("&tag=" + tag) : "");
  }

  private String getDataFetchUrl(CVConfiguration cvConfiguration, long startMinute, long endMinute, String tag) {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/get-metric-data-247?accountId=" + cvConfiguration.getAccountId() + "&appId=" + cvConfiguration.getAppId()
        + "&stateType=" + cvConfiguration.getStateType() + "&cvConfigId=" + cvConfiguration.getUuid()
        + "&serviceId=" + cvConfiguration.getServiceId() + "&analysisStartMin=" + startMinute
        + "&analysisEndMin=" + endMinute + (tag != null ? ("&tag=" + tag) : "");
  }

  private String getPreviousAnalysisUrl(CVConfiguration cvConfiguration, String tag) {
    long min = timeSeriesAnalysisService.getLastCVAnalysisMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/previous-analysis-247?appId=" + cvConfiguration.getAppId() + "&cvConfigId=" + cvConfiguration.getUuid()
        + "&dataCollectionMin=" + min + (tag != null ? ("&tag=" + tag) : "");
  }

  private String getHistoricalAnalysisUrl(CVConfiguration cvConfiguration, long minute, String tag) {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL
        + "/historical-analysis-24x7?accountId=" + cvConfiguration.getAccountId() + "&applicationId="
        + cvConfiguration.getAppId() + "&serviceId=" + cvConfiguration.getServiceId() + "&analysisMinute=" + minute
        + "&cvConfigId=" + cvConfiguration.getUuid() + (tag != null ? ("&tag=" + tag) : "");
  }

  private String getPreviousAnomaliesUrl(CVConfiguration cvConfiguration, String tag) {
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/previous-anomalies-247"
        + "?accountId=" + cvConfiguration.getAccountId() + "&applicationId=" + cvConfiguration.getAppId()
        + "&cvConfigId=" + cvConfiguration.getUuid() + (tag != null ? ("&tag=" + tag) : "");
  }

  private String getCumulativeSumsUrl(CVConfiguration cvConfiguration, int analysisMinute, String tag) {
    int startMin = analysisMinute - (int) TimeUnit.DAYS.toMinutes(1);
    return VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/cumulative-sums-247"
        + "?accountId=" + cvConfiguration.getAccountId() + "&applicationId=" + cvConfiguration.getAppId()
        + "&cvConfigId=" + cvConfiguration.getUuid() + "&analysisMinStart=" + startMin
        + "&analysisMinEnd=" + analysisMinute + (tag != null ? ("&tag=" + tag) : "");
  }

  @Override
  @Counted
  @Timed
  public boolean triggerLogDataCollection(String accountId) {
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);
    long endMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - TIME_DELAY_QUERY_MINS;
    AtomicLong totalDataCollectionTasks = new AtomicLong(0);
    cvConfigurations.stream()
        .filter(cvConfiguration
            -> cvConfiguration.isEnabled24x7() && getLogAnalysisStates().contains(cvConfiguration.getStateType()))
        .forEach(cvConfiguration -> {
          try {
            LogsCVConfiguration logsCVConfiguration = (LogsCVConfiguration) cvConfiguration;
            if (logsCVConfiguration.getBaselineStartMinute() < 0 || logsCVConfiguration.getBaselineEndMinute() < 0) {
              logger.error("For {} baseline is not set. Skipping collection", logsCVConfiguration.getUuid());
              return;
            }
            final long maxCVCollectionMinute = logAnalysisService.getMaxCVCollectionMinute(
                logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid());

            long startTime = maxCVCollectionMinute <= 0
                ? TimeUnit.MINUTES.toMillis(logsCVConfiguration.getBaselineStartMinute())
                : TimeUnit.MINUTES.toMillis(maxCVCollectionMinute + 1);
            long endTime = startTime + TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES - 1);

            if (PREDICTIVE.equals(cvConfiguration.getComparisonStrategy())
                && maxCVCollectionMinute >= logsCVConfiguration.getBaselineEndMinute()) {
              AnalysisContext analysisContext =
                  wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
              endTime = startTime + TimeUnit.MINUTES.toMillis(1);

              if (maxCVCollectionMinute
                  >= logsCVConfiguration.getBaselineEndMinute() + analysisContext.getTimeDuration()) {
                logger.info("collection for {} is done", analysisContext.getStateExecutionId());
                return;
              }
            }

            if (endTime < TimeUnit.MINUTES.toMillis(endMinute)) {
              logger.info(
                  "triggering data collection for state {} config {} startTime {} endTime {} collectionMinute {}",
                  cvConfiguration.getStateType(), cvConfiguration.getUuid(), startTime, endTime, endMinute);
              verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerCVDataCollection(
                  cvConfiguration.getUuid(), cvConfiguration.getStateType(), startTime, endTime));
              totalDataCollectionTasks.getAndIncrement();
            }
          } catch (Exception ex) {
            logger.error(
                "Exception occurred while triggering datacollection for cvConfig {}", cvConfiguration.getUuid(), ex);
          }
        });
    metricRegistry.recordGaugeValue(DATA_COLLECTION_TASKS_PER_MINUTE, null, totalDataCollectionTasks.get());
    return true;
  }

  @Override
  @Counted
  @Timed
  public boolean triggerLogDataCollection(AnalysisContext context) {
    final long lastDataCollectionMinute = logAnalysisService.getLastLogDataCollectedMinute(
        context.getQuery(), context.getAppId(), context.getStateExecutionId(), context.getStateType());
    logger.info("Inside triggerLogDataCollection with stateType {}, stateExecutionId {}", context.getStateType(),
        context.getStateExecutionId());
    boolean isStateValid = verificationManagerClientHelper
                               .callManagerWithRetry(verificationManagerClient.isStateValid(
                                   context.getAppId(), context.getStateExecutionId()))
                               .getResource();
    if (isStateValid) {
      try {
        if (lastDataCollectionMinute < 0) {
          return verificationManagerClientHelper
              .callManagerWithRetry(verificationManagerClient.triggerWorkflowDataCollection(
                  context.getUuid(), context.getStartDataCollectionMinute()))
              .getResource();
        } else {
          if (lastDataCollectionMinute - context.getStartDataCollectionMinute() < context.getTimeDuration() - 1) {
            logger.info("Trigger Log Data Collection with stateType {}, stateExecutionId {}", context.getStateType(),
                context.getStateExecutionId());
            return verificationManagerClientHelper
                .callManagerWithRetry(verificationManagerClient.triggerWorkflowDataCollection(
                    context.getUuid(), lastDataCollectionMinute + 1))
                .getResource();
          } else {
            logger.info("Completed Log Data Collection for stateType {}, stateExecutionId {}", context.getStateType(),
                context.getStateExecutionId());
            return false;
          }
        }
      } catch (Exception e) {
        logger.error("Failed to call manager for log data collection for workflow with context {} with exception {}",
            context, e);
      }
      return true;
    } else {
      logger.info("State is no longer valid for stateType {}, stateExecutionId {}", context.getStateType(),
          context.getStateExecutionId());
      return false;
    }
  }

  @Override
  @Counted
  @Timed
  public void triggerLogsL1Clustering(String accountId) {
    // List all the CV configurations for a given account
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);

    // for each 24x7 enabled configurations do following
    // Get last CV Analysis minute on given configuration
    // Add new learning task for next ANALYSIS_PERIOD_MINUTES period
    cvConfigurations.stream()
        .filter(cvConfiguration
            -> cvConfiguration.isEnabled24x7() && getLogAnalysisStates().contains(cvConfiguration.getStateType())
                && !cvConfiguration.getStateType().equals(StateType.SPLUNKV2))
        .forEach(cvConfiguration -> {
          logger.info(
              "triggering logs L1 Clustering for account {} and cvConfigId {}", accountId, cvConfiguration.getUuid());
          long lastCVDataCollectionMinute =
              logAnalysisService.getMaxCVCollectionMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
          if (lastCVDataCollectionMinute <= 0) {
            logger.info(
                "For account {} and CV config {} name {} type {} no data has been collected yet. Skipping clustering",
                cvConfiguration.getAccountId(), cvConfiguration.getUuid(), cvConfiguration.getName(),
                cvConfiguration.getStateType());
            return;
          }

          long minLogRecordMinute = logAnalysisService.getLogRecordMinute(
              cvConfiguration.getAppId(), cvConfiguration.getUuid(), ClusterLevel.H0, OrderType.ASC);
          for (long logRecordMinute = minLogRecordMinute;
               logRecordMinute > 0 && logRecordMinute <= lastCVDataCollectionMinute; logRecordMinute++) {
            Set<String> hosts = logAnalysisService.getHostsForMinute(
                cvConfiguration.getAppId(), cvConfiguration.getUuid(), logRecordMinute, ClusterLevel.L0);
            String inputLogsUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                + LogAnalysisResource.ANALYSIS_GET_24X7_LOG_URL + "?cvConfigId=" + cvConfiguration.getUuid()
                + "&appId=" + cvConfiguration.getAppId() + "&clusterLevel=" + ClusterLevel.L0
                + "&logCollectionMinute=" + logRecordMinute;
            String clusteredLogSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                + LogAnalysisResource.ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL
                + "?cvConfigId=" + cvConfiguration.getUuid() + "&appId=" + cvConfiguration.getAppId()
                + "&clusterLevel=" + ClusterLevel.L1 + "&logCollectionMinute=" + logRecordMinute;

            String stateExecutionIdForLETask = "LOGS_CLUSTER_L1_" + cvConfiguration.getUuid() + "_" + logRecordMinute;
            learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) logRecordMinute);
            LearningEngineAnalysisTask analysisTask =
                LearningEngineAnalysisTask.builder()
                    .control_input_url(inputLogsUrl)
                    .analysis_save_url(clusteredLogSaveUrl)
                    .state_execution_id(stateExecutionIdForLETask)
                    .service_id(cvConfiguration.getServiceId())
                    .control_nodes(hosts)
                    .sim_threshold(0.99)
                    .analysis_minute(logRecordMinute)
                    .cluster_level(ClusterLevel.L1.getLevel())
                    .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                    .stateType(cvConfiguration.getStateType())
                    .query(Lists.newArrayList(((LogsCVConfiguration) cvConfiguration).getQuery()))
                    .is24x7Task(true)
                    .cvConfigId(cvConfiguration.getUuid())
                    .build();
            analysisTask.setAppId(cvConfiguration.getAppId());

            final boolean taskQueued = learningEngineService.addLearningEngineAnalysisTask(analysisTask);
            if (taskQueued) {
              logger.info("L1 Clustering queued for cvConfig {} for hosts {} for minute {}", cvConfiguration.getUuid(),
                  hosts, logRecordMinute);
            }

            List<MLExperiments> experiments = get24x7Experiments();
            for (MLExperiments experiment : experiments) {
              LearningEngineExperimentalAnalysisTask expTask =
                  LearningEngineExperimentalAnalysisTask.builder()
                      .control_input_url(inputLogsUrl)
                      .analysis_save_url(getSaveUrlForExperimentalTask())
                      .state_execution_id(
                          "LOGS_CLUSTER_L1_" + cvConfiguration.getUuid() + "_" + logRecordMinute + generateUuid())
                      .service_id(cvConfiguration.getServiceId())
                      .control_nodes(hosts)
                      .sim_threshold(0.99)
                      .analysis_minute(logRecordMinute)
                      .cluster_level(ClusterLevel.L1.getLevel())
                      .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                      .stateType(cvConfiguration.getStateType())
                      .query(Lists.newArrayList(((LogsCVConfiguration) cvConfiguration).getQuery()))
                      .is24x7Task(true)
                      .cvConfigId(cvConfiguration.getUuid())
                      .experiment_name(experiment.getExperimentName())
                      .build();
              expTask.setAppId(cvConfiguration.getAppId());
              learningEngineService.addLearningEngineExperimentalAnalysisTask(expTask);
            }
          }
        });
  }

  @Override
  @Counted
  @Timed
  public void triggerLogsL2Clustering(String accountId) {
    // List all the CV configurations for a given account
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);

    // for each 24x7 enabled configurations do following
    // Get last CV Analysis minute on given configuration
    // Add new learning task for next ANALYSIS_PERIOD_MINUTES period
    cvConfigurations.stream()
        .filter(cvConfiguration
            -> cvConfiguration.isEnabled24x7() && getLogAnalysisStates().contains(cvConfiguration.getStateType())
                && !cvConfiguration.getStateType().equals(StateType.SPLUNKV2))
        .forEach(cvConfiguration -> {
          logger.info(
              "triggering logs L2 Clustering for account {} and cvConfigId {}", accountId, cvConfiguration.getUuid());
          try {
            long minLogRecordL1Minute = logAnalysisService.getLogRecordMinute(
                cvConfiguration.getAppId(), cvConfiguration.getUuid(), ClusterLevel.H1, OrderType.ASC);

            if (minLogRecordL1Minute <= 0) {
              logger.info(
                  "For account {} and CV config {} name {} type {} no data L1 clustering has happened yet. Skipping L2 clustering",
                  cvConfiguration.getAccountId(), cvConfiguration.getUuid(), cvConfiguration.getName(),
                  cvConfiguration.getStateType());
              return;
            }

            long maxLogRecordL1Minute = logAnalysisService.getLogRecordMinute(
                cvConfiguration.getAppId(), cvConfiguration.getUuid(), ClusterLevel.H1, OrderType.DESC);

            if (!AnalysisComparisonStrategy.PREDICTIVE.equals(cvConfiguration.getComparisonStrategy())
                && maxLogRecordL1Minute < minLogRecordL1Minute + CRON_POLL_INTERVAL_IN_MINUTES - 1) {
              logger.info(
                  "For CV config {} there is still node data clustering is pending. min l1 {} max l1 {}. Skipping L2 clustering",
                  cvConfiguration.getUuid(), minLogRecordL1Minute, maxLogRecordL1Minute);
              return;
            }
            maxLogRecordL1Minute = minLogRecordL1Minute + CRON_POLL_INTERVAL_IN_MINUTES - 1;

            if (PREDICTIVE.equals(cvConfiguration.getComparisonStrategy())
                && minLogRecordL1Minute >= ((LogsCVConfiguration) cvConfiguration).getBaselineEndMinute()) {
              maxLogRecordL1Minute = minLogRecordL1Minute + 1;
            }

            for (long logRecordMinute = minLogRecordL1Minute; logRecordMinute < maxLogRecordL1Minute;
                 logRecordMinute++) {
              Set<String> hosts = logAnalysisService.getHostsForMinute(
                  cvConfiguration.getAppId(), cvConfiguration.getUuid(), logRecordMinute, ClusterLevel.L0);
              if (isNotEmpty(hosts)) {
                logger.info(
                    "For CV config {} there is still node data clustering is pending for {} for minute {}. Skipping L2 clustering",
                    cvConfiguration.getUuid(), hosts, logRecordMinute);
                return;
              }

              hosts = logAnalysisService.getHostsForMinute(
                  cvConfiguration.getAppId(), cvConfiguration.getUuid(), logRecordMinute, ClusterLevel.L1);
              if (isEmpty(hosts)) {
                logger.info(
                    "For CV config {} there is no clustering data present for minute {}. Skipping L2 clustering",
                    cvConfiguration.getUuid(), logRecordMinute);
                return;
              }
            }
            logger.info("for {} for minute from {} to {} everything is in place, proceeding for L2 Clustering",
                cvConfiguration.getUuid(), minLogRecordL1Minute, maxLogRecordL1Minute);

            String inputLogsUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                + LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL + "?cvConfigId=" + cvConfiguration.getUuid()
                + "&appId=" + cvConfiguration.getAppId() + "&clusterLevel=" + ClusterLevel.L1
                + "&startMinute=" + minLogRecordL1Minute + "&endMinute=" + maxLogRecordL1Minute;
            String clusteredLogSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                + LogAnalysisResource.ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL
                + "?cvConfigId=" + cvConfiguration.getUuid() + "&appId=" + cvConfiguration.getAppId()
                + "&clusterLevel=" + ClusterLevel.L2 + "&logCollectionMinute=" + maxLogRecordL1Minute;

            String stateExecutionIdForLETask =
                "LOGS_CLUSTER_L2_" + cvConfiguration.getUuid() + "_" + maxLogRecordL1Minute;
            learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) maxLogRecordL1Minute);
            LearningEngineAnalysisTask analysisTask =
                LearningEngineAnalysisTask.builder()
                    .control_input_url(inputLogsUrl)
                    .analysis_save_url(clusteredLogSaveUrl)
                    .state_execution_id(stateExecutionIdForLETask)
                    .service_id(cvConfiguration.getServiceId())
                    .control_nodes(Collections.emptySet())
                    .sim_threshold(0.99)
                    .analysis_minute(maxLogRecordL1Minute)
                    .cluster_level(ClusterLevel.L2.getLevel())
                    .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                    .stateType(cvConfiguration.getStateType())
                    .query(Lists.newArrayList(((LogsCVConfiguration) cvConfiguration).getQuery()))
                    .is24x7Task(true)
                    .cvConfigId(cvConfiguration.getUuid())
                    .build();
            analysisTask.setAppId(cvConfiguration.getAppId());

            final boolean taskQueued = learningEngineService.addLearningEngineAnalysisTask(analysisTask);
            if (taskQueued) {
              logger.info("L2 Clustering queued for cvConfig {} from minute {} to minute {}", cvConfiguration.getUuid(),
                  minLogRecordL1Minute, maxLogRecordL1Minute);
            }
            List<MLExperiments> experiments = get24x7Experiments();
            for (MLExperiments experiment : experiments) {
              LearningEngineExperimentalAnalysisTask expTask =
                  LearningEngineExperimentalAnalysisTask.builder()
                      .control_input_url(inputLogsUrl)
                      .analysis_save_url(getSaveUrlForExperimentalTask())
                      .state_execution_id("LOGS_CLUSTER_L2_" + cvConfiguration.getUuid() + "_" + maxLogRecordL1Minute
                          + "-" + generateUUID())
                      .service_id(cvConfiguration.getServiceId())
                      .control_nodes(Collections.emptySet())
                      .sim_threshold(0.99)
                      .analysis_minute(maxLogRecordL1Minute)
                      .cluster_level(ClusterLevel.L2.getLevel())
                      .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                      .stateType(cvConfiguration.getStateType())
                      .query(Lists.newArrayList(((LogsCVConfiguration) cvConfiguration).getQuery()))
                      .is24x7Task(true)
                      .cvConfigId(cvConfiguration.getUuid())
                      .experiment_name(experiment.getExperimentName())
                      .build();
              expTask.setAppId(cvConfiguration.getAppId());
              learningEngineService.addLearningEngineExperimentalAnalysisTask(expTask);
            }
          } catch (Exception ex) {
            logger.error("Creating L2 task failed for cvConfig " + cvConfiguration.getUuid());
          }
        });
  }

  @Override
  @Counted
  @Timed
  public void triggerLogDataAnalysis(String accountId) {
    // List all the CV configurations for a given account
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);

    // for each 24x7 enabled configurations do following
    // Get last CV Analysis minute on given configuration
    // Add new learning task for next ANALYSIS_PERIOD_MINUTES period
    cvConfigurations.stream()
        .filter(cvConfiguration
            -> cvConfiguration.isEnabled24x7() && getLogAnalysisStates().contains(cvConfiguration.getStateType()))

        .forEach(cvConfiguration -> {
          logger.info(
              "triggering logs Data Analysis for account {} and cvConfigId {}", accountId, cvConfiguration.getUuid());
          LogsCVConfiguration logsCVConfiguration = (LogsCVConfiguration) cvConfiguration;

          try {
            if (logsCVConfiguration.isWorkflowConfig()) {
              AnalysisContext context = wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
              long analysisLastMin = logAnalysisService.getLogRecordMinute(
                  logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid(), ClusterLevel.HF, OrderType.DESC);
              if (analysisLastMin >= logsCVConfiguration.getBaselineEndMinute() + context.getTimeDuration()) {
                logger.info(
                    "Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
                sendStateNotification(context, false, "", (int) analysisLastMin);
                logger.info("Disabled 24x7 for CV Configuration with id {}", logsCVConfiguration.getUuid());
                wingsPersistence.updateField(
                    LogsCVConfiguration.class, logsCVConfiguration.getUuid(), "enabled24x7", false);
                return;
              }
            }
            long analysisStartMin = logAnalysisService.getLogRecordMinute(
                logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid(), ClusterLevel.H2, OrderType.ASC);
            if (analysisStartMin <= 0) {
              logger.info(
                  "For account {} and CV config {} name {} type {} no data L2 clustering has happened yet. Skipping analysis",
                  logsCVConfiguration.getAccountId(), logsCVConfiguration.getUuid(), logsCVConfiguration.getName(),
                  logsCVConfiguration.getStateType());
              return;
            }

            long lastCVAnalysisMinute = logAnalysisService.getLastCVAnalysisMinute(
                logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid());

            if (lastCVAnalysisMinute > analysisStartMin) {
              logger.info("for {} last analysis happened for min {}, will try again for {} soon",
                  logsCVConfiguration.getUuid(), lastCVAnalysisMinute, analysisStartMin);
              return;
            }
            long analysisEndMin = analysisStartMin + CRON_POLL_INTERVAL_IN_MINUTES - 1;

            if (logsCVConfiguration.isWorkflowConfig()) {
              AnalysisContext context = wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
              if (lastCVAnalysisMinute >= logsCVConfiguration.getBaselineEndMinute()) {
                analysisEndMin = lastCVAnalysisMinute + 1;
              }

              if (analysisEndMin > logsCVConfiguration.getBaselineEndMinute() + context.getTimeDuration()) {
                logger.info(
                    "Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
                sendStateNotification(context, false, "", (int) analysisEndMin);
                logger.info("Disabled 24x7 for CV Configuration with id {}", logsCVConfiguration.getUuid());
                wingsPersistence.updateField(
                    LogsCVConfiguration.class, logsCVConfiguration.getUuid(), "enabled24x7", false);
              }
            }

            for (long l2Min = analysisStartMin, i = 0; l2Min <= analysisEndMin; l2Min++, i++) {
              Set<String> hosts = logAnalysisService.getHostsForMinute(
                  cvConfiguration.getAppId(), cvConfiguration.getUuid(), l2Min, ClusterLevel.L1);
              if (isNotEmpty(hosts)) {
                logger.info(
                    "For CV config {} there is still L2 clustering pending for {} for minute {}. Skipping L2 Analysis",
                    cvConfiguration.getUuid(), hosts, l2Min);
                return;
              }
            }

            logger.info("for {} for minute from {} to {} everything is in place, proceeding for analysis",
                logsCVConfiguration.getUuid(), analysisStartMin, analysisEndMin);

            String taskId = generateUuid();

            String controlInputUrl = null;
            String testInputUrl = null;
            boolean isBaselineRun = false;
            // this is the baseline prep case
            if (analysisStartMin < logsCVConfiguration.getBaselineStartMinute()
                || (analysisStartMin >= logsCVConfiguration.getBaselineStartMinute()
                       && analysisStartMin < logsCVConfiguration.getBaselineEndMinute())) {
              controlInputUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL + "?cvConfigId=" + logsCVConfiguration.getUuid()
                  + "&appId=" + logsCVConfiguration.getAppId() + "&clusterLevel=" + ClusterLevel.L2
                  + "&startMinute=" + analysisStartMin + "&endMinute=" + analysisEndMin;
              isBaselineRun = true;
            } else {
              testInputUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL + "?cvConfigId=" + logsCVConfiguration.getUuid()
                  + "&appId=" + logsCVConfiguration.getAppId() + "&clusterLevel=" + ClusterLevel.L2
                  + "&startMinute=" + analysisStartMin + "&endMinute=" + analysisEndMin;
            }

            String logAnalysisSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                + LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL
                + "?cvConfigId=" + logsCVConfiguration.getUuid() + "&appId=" + logsCVConfiguration.getAppId()
                + "&analysisMinute=" + analysisEndMin + "&taskId=" + taskId
                + "&comparisonStrategy=" + logsCVConfiguration.getComparisonStrategy();

            final String logAnalysisGetUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                + LogAnalysisResource.ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL
                + "?appId=" + logsCVConfiguration.getAppId() + "&cvConfigId=" + logsCVConfiguration.getUuid()
                + "&analysisMinute=" + ((LogsCVConfiguration) cvConfiguration).getBaselineEndMinute();

            String stateExecutionIdForLETask =
                "LOG_24X7_ANALYSIS_" + logsCVConfiguration.getUuid() + "_" + analysisEndMin;
            learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) analysisEndMin);
            LearningEngineAnalysisTask analysisTask =
                LearningEngineAnalysisTask.builder()
                    .state_execution_id(stateExecutionIdForLETask)
                    .service_id(logsCVConfiguration.getServiceId())
                    .query(Lists.newArrayList(logsCVConfiguration.getQuery()))
                    .sim_threshold(0.9)
                    .analysis_minute(analysisEndMin)
                    .analysis_save_url(logAnalysisSaveUrl)
                    .log_analysis_get_url(logAnalysisGetUrl)
                    .ml_analysis_type(MLAnalysisType.LOG_ML)
                    .test_input_url(testInputUrl)
                    .control_input_url(controlInputUrl)
                    .test_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
                    .feature_name("NEURAL_NET")
                    .is24x7Task(true)
                    .stateType(logsCVConfiguration.getStateType())
                    .tolerance(cvConfiguration.getAnalysisTolerance().tolerance())
                    .cvConfigId(logsCVConfiguration.getUuid())
                    .analysis_comparison_strategy(logsCVConfiguration.getComparisonStrategy())
                    .build();

            analysisTask.setAppId(logsCVConfiguration.getAppId());
            analysisTask.setUuid(taskId);
            if (isBaselineRun) {
              analysisTask.setValidUntil(Date.from(OffsetDateTime.now().plusMonths(6).toInstant()));
            }
            if (logsCVConfiguration.getComparisonStrategy() == PREDICTIVE) {
              final String lastLogAnalysisGetUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL
                  + "?appId=" + logsCVConfiguration.getAppId() + "&cvConfigId=" + logsCVConfiguration.getUuid()
                  + "&analysisMinute=" + analysisEndMin;
              analysisTask.setPrevious_test_analysis_url(lastLogAnalysisGetUrl);
            }
            learningEngineService.addLearningEngineAnalysisTask(analysisTask);

            if (lastCVAnalysisMinute <= 0) {
              logger.info(
                  "For account {} and CV config {} name {} type {} no analysis has been done yet. This is going to be first analysis",
                  logsCVConfiguration.getAccountId(), logsCVConfiguration.getUuid(), logsCVConfiguration.getName(),
                  logsCVConfiguration.getStateType());
            }

            logger.info("Queuing analysis task for state {} config {} with analysisMin {}",
                logsCVConfiguration.getStateType(), logsCVConfiguration.getUuid(), analysisEndMin);

            List<MLExperiments> experiments = get24x7Experiments();
            for (MLExperiments experiment : experiments) {
              LearningEngineExperimentalAnalysisTask expTask =
                  LearningEngineExperimentalAnalysisTask.builder()
                      .state_execution_id(
                          "LOG_24X7_ANALYSIS_" + logsCVConfiguration.getUuid() + "_" + analysisEndMin + generateUuid())
                      .service_id(logsCVConfiguration.getServiceId())
                      .query(Lists.newArrayList(logsCVConfiguration.getQuery()))
                      .sim_threshold(0.9)
                      .analysis_minute(analysisEndMin)
                      .analysis_save_url(getSaveUrlForExperimentalTask())
                      .log_analysis_get_url(logAnalysisGetUrl)
                      .ml_analysis_type(MLAnalysisType.LOG_ML)
                      .test_input_url(testInputUrl)
                      .control_input_url(controlInputUrl)
                      .test_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
                      .feature_name("NEURAL_NET")
                      .is24x7Task(true)
                      .stateType(logsCVConfiguration.getStateType())
                      .tolerance(cvConfiguration.getAnalysisTolerance().tolerance())
                      .cvConfigId(logsCVConfiguration.getUuid())
                      .analysis_comparison_strategy(logsCVConfiguration.getComparisonStrategy())
                      .experiment_name(experiment.getExperimentName())
                      .build();
              expTask.setAppId(cvConfiguration.getAppId());
              learningEngineService.addLearningEngineExperimentalAnalysisTask(expTask);
            }

          } catch (Exception ex) {
            try {
              if (cvConfiguration.isWorkflowConfig()) {
                AnalysisContext context =
                    wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
                logger.error("Verification L1 => L2 cluster failed", ex);
                final LogAnalysisExecutionData executionData = LogAnalysisExecutionData.builder().build();
                executionData.setStatus(ExecutionStatus.ERROR);
                executionData.setErrorMsg(ex.getMessage());
                logger.info(
                    "Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
                verificationManagerClientHelper.notifyManagerForLogAnalysis(context,
                    aLogAnalysisResponse()
                        .withLogAnalysisExecutionData(executionData)
                        .withExecutionStatus(ExecutionStatus.ERROR)
                        .build());
                wingsPersistence.updateField(CVConfiguration.class, cvConfiguration.getUuid(), "enabled24x7", false);
              }
            } catch (Exception e) {
              logger.error("Verification cluster manager cleanup failed", e);
            }
          }
        });
  }

  @Override
  public void cleanupStuckLocks() {
    DBCollection collection =
        wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "quartz_verification_locks");
    DBCursor lockDataRecords = collection.find();

    logger.info("will go through " + lockDataRecords.size() + " records");

    List<ObjectId> toBeDeleted = new ArrayList<>();
    while (lockDataRecords.hasNext()) {
      DBObject next = lockDataRecords.next();

      Date time = (Date) next.get("time");
      long lockTime = time.getTime();
      if (lockTime < System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)) {
        toBeDeleted.add((ObjectId) next.get(ID_KEY));
      }
    }
    if (isNotEmpty(toBeDeleted)) {
      logger.info("deleting locks {}", toBeDeleted);
      collection.remove(new BasicDBObject(ID_KEY, new BasicDBObject("$in", toBeDeleted.toArray())));
    }
  }

  private void sendStateNotification(AnalysisContext context, boolean error, String errorMsg, int logAnalysisMinute) {
    if (analysisService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
      final ExecutionStatus status = error ? ExecutionStatus.ERROR : ExecutionStatus.SUCCESS;

      LogAnalysisExecutionData logAnalysisExecutionData =
          LogAnalysisExecutionData.builder()
              .stateExecutionInstanceId(context.getStateExecutionId())
              .serverConfigId(context.getAnalysisServerConfigId())
              .query(context.getQuery())
              .timeDuration(context.getTimeDuration())
              .canaryNewHostNames(context.getTestNodes().keySet())
              .lastExecutionNodes(
                  context.getControlNodes() == null ? Collections.emptySet() : context.getControlNodes().keySet())
              .correlationId(context.getCorrelationId())
              .analysisMinute(logAnalysisMinute)
              .build();

      logAnalysisExecutionData.setStatus(status);

      if (error) {
        logAnalysisExecutionData.setErrorMsg(errorMsg);
      }

      final LogAnalysisResponse response = aLogAnalysisResponse()
                                               .withLogAnalysisExecutionData(logAnalysisExecutionData)
                                               .withExecutionStatus(status)
                                               .build();
      logger.info("Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
      verificationManagerClientHelper.notifyManagerForLogAnalysis(context, response);
    }
  }

  @Override
  public void triggerAlertIfNecessary(String cvConfigId, double riskScore, long analysisMinute) {
    if (isEmpty(cvConfigId)) {
      return;
    }
    final CVConfiguration cvConfiguration = wingsPersistence.get(CVConfiguration.class, cvConfigId);
    Preconditions.checkNotNull(cvConfiguration, "no config found with id " + cvConfigId);

    if (!cvConfiguration.isAlertEnabled()) {
      logger.info("for {} the alert is not enabled. Returning", cvConfigId);
      return;
    }

    if (riskScore <= cvConfiguration.getAlertThreshold()) {
      logger.info("for {} the risk {} is lower than the threshold {}. No alerts will be triggered.", cvConfigId,
          riskScore, cvConfiguration.getAlertThreshold());
      return;
    }

    final long currentTime = System.currentTimeMillis();
    if (cvConfiguration.getSnoozeStartTime() > 0 && cvConfiguration.getSnoozeEndTime() > 0
        && currentTime >= cvConfiguration.getSnoozeStartTime() && currentTime <= cvConfiguration.getSnoozeEndTime()) {
      logger.info("for {} the current time is in the range of snooze time {} to {}. No alerts will be triggered.",
          cvConfigId, cvConfiguration.getSnoozeStartTime(), cvConfiguration.getSnoozeEndTime());
      return;
    }

    logger.info("triggering alert for {} with risk score {}", cvConfigId, riskScore);
    verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerCVAlert(cvConfigId,
        ContinuousVerificationAlertData.builder()
            .riskScore(riskScore)
            .analysisStartTime(TimeUnit.MINUTES.toMillis(analysisMinute - CRON_POLL_INTERVAL_IN_MINUTES))
            .analysisEndTime(TimeUnit.MINUTES.toMillis(analysisMinute))
            .build()));
  }
}
