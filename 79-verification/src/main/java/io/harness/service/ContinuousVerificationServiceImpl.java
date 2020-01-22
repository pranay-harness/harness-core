package io.harness.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.common.VerificationConstants.DATA_COLLECTION_TASKS_PER_MINUTE;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.common.VerificationConstants.GET_LOG_FEEDBACKS;
import static software.wings.common.VerificationConstants.IS_EXPERIMENTAL;
import static software.wings.common.VerificationConstants.TIME_DELAY_QUERY_MINS;
import static software.wings.common.VerificationConstants.VERIFICATION_SERVICE_BASE_URL;
import static software.wings.common.VerificationConstants.getLogAnalysisStates;
import static software.wings.common.VerificationConstants.getMetricAnalysisStates;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.PREDICTIVE;
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
import io.harness.entities.CVTask;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.bson.types.ObjectId;
import software.wings.alerts.AlertStatus;
import software.wings.beans.FeatureName;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.VerificationLogContext;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.FeedbackAction;
import software.wings.service.impl.analysis.LogDataRecord.LogDataRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.analysis.SupervisedTrainingStatus.SupervisedTrainingStatusKeys;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates.TimeSeriesMetricTemplatesKeys;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.impl.newrelic.MLExperiments.MLExperimentsKeys;
import software.wings.service.impl.splunk.LogAnalysisResult;
import software.wings.service.impl.splunk.SplunkAnalysisCluster;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.verification.CVTaskService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.SplunkCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by rsingh on 10/9/18.
 */
@Slf4j
public class ContinuousVerificationServiceImpl implements ContinuousVerificationService {
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
  @Inject private CVTaskService cvTaskService;
  @Inject private CVActivityLogService cvActivityLogService;

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
          long startTime = getDataCollectionStartMinForAPM(cvConfiguration, endMinute);
          long endTime = TimeUnit.MINUTES.toMillis(endMinute);
          if (endTime - startTime >= TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES / 3)) {
            logger.info("triggering data collection for state {} config {} startTime {} endTime {} collectionMinute {}",
                cvConfiguration.getStateType(), cvConfiguration.getUuid(), startTime, endTime, endMinute);
            if (isCVTaskBasedCollectionEnabled(cvConfiguration)) {
              createCVTask(cvConfiguration, startTime, endTime);
            } else {
              verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerCVDataCollection(
                  cvConfiguration.getUuid(), cvConfiguration.getStateType(), startTime, endTime));
              totalDataCollectionTasks.getAndIncrement();
            }
          }
        });
    metricRegistry.recordGaugeValue(DATA_COLLECTION_TASKS_PER_MINUTE, null, totalDataCollectionTasks.get());
    return true;
  }

  private long getDataCollectionStartMinForAPM(CVConfiguration cvConfiguration, long endMinute) {
    long maxCVCollectionMinute = timeSeriesAnalysisService.getMaxCVCollectionMinute(
        cvConfiguration.getAppId(), cvConfiguration.getUuid(), cvConfiguration.getAccountId());
    long startTime;
    if (maxCVCollectionMinute <= 0) {
      // no collection has been done so far
      startTime = TimeUnit.MINUTES.toMillis(endMinute) - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES)
          - TimeUnit.SECONDS.toMillis(CRON_POLL_INTERVAL);
    } else if (endMinute - maxCVCollectionMinute > PREDECTIVE_HISTORY_MINUTES) {
      // the last collection was more than 2 hours ago.
      // So our analysis is going to start from 2hours (with 2 hours before that as history)
      startTime = TimeUnit.MINUTES.toMillis(endMinute - PREDECTIVE_HISTORY_MINUTES * 2);
      logger.info(
          "The last datacollection for {} was more than 2 hours ago, so we will restart from 2hours. Setting start time to {}",
          cvConfiguration.getUuid(), startTime);
    } else {
      // All is well. Happy case.
      startTime = TimeUnit.MINUTES.toMillis(maxCVCollectionMinute);
    }
    return startTime;
  }

  private long getAnalysisStartMinuteForAPM(CVConfiguration cvConfiguration, long lastCVDataCollectionMinute) {
    long lastCVAnalysisMinute =
        timeSeriesAnalysisService.getLastCVAnalysisMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    if (lastCVAnalysisMinute <= 0) {
      logger.info(
          "For account {} and CV config {} name {} type {} no analysis has been done yet. This is going to be first analysis",
          cvConfiguration.getAccountId(), cvConfiguration.getUuid(), cvConfiguration.getName(),
          cvConfiguration.getStateType());
      return lastCVDataCollectionMinute - TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL);
    } else if (lastCVAnalysisMinute + PREDECTIVE_HISTORY_MINUTES < currentMinute) {
      // it has been more than 2 hours since we did analysis, so we should just do for current time - 2hours and take
      // over from there.
      logger.info("The last analysis was more than 2 hours ago. We're restarting the analysis from minute: {} for {}",
          currentMinute - PREDECTIVE_HISTORY_MINUTES, cvConfiguration.getUuid());
      long restartTime = currentMinute - PREDECTIVE_HISTORY_MINUTES;
      if ((restartTime - lastCVAnalysisMinute) % CRON_POLL_INTERVAL_IN_MINUTES != 0) {
        restartTime -= (restartTime - lastCVAnalysisMinute) % CRON_POLL_INTERVAL_IN_MINUTES;
      }
      // check to see if there was any task created for this cvConfig for one hour before expected restart time. If yes,
      // dont do this. Return -1
      boolean isTaskRunning = learningEngineService.isTaskRunningOrQueued(
          cvConfiguration.getUuid(), restartTime - PREDECTIVE_HISTORY_MINUTES - 60);
      if (isTaskRunning) {
        return -1;
      }
      return restartTime;
    } else {
      return lastCVAnalysisMinute;
    }
  }
  @Override
  @Counted
  @Timed
  public void triggerServiceGuardTimeSeriesAnalysis(String accountId) {
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
          try (VerificationLogContext ignored = new VerificationLogContext(cvConfiguration.getAccountId(),
                   cvConfiguration.getUuid(), null, cvConfiguration.getStateType(), OVERRIDE_ERROR)) {
            try {
              logger.info("Executing APM data analysis Job for accountId {} and configId {}", accountId,
                  cvConfiguration.getUuid());
              long lastCVDataCollectionMinute = timeSeriesAnalysisService.getMaxCVCollectionMinute(
                  cvConfiguration.getAppId(), cvConfiguration.getUuid(), cvConfiguration.getAccountId());
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
              long analysisStartMinute = getAnalysisStartMinuteForAPM(cvConfiguration, lastCVDataCollectionMinute);
              if (analysisStartMinute == -1) {
                logger.info(
                    "The last analysis was more than 2 hours ago but there is currently a task running for {}. So exiting.",
                    cvConfiguration.getUuid());
                return;
              }
              if (lastCVDataCollectionMinute - lastCVAnalysisMinute >= TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL)) {
                long endMinute = analysisStartMinute + TimeUnit.SECONDS.toMinutes(CRON_POLL_INTERVAL);

                // since analysis startMin is inclusive in LE, we  need to add 1.
                analysisStartMinute += 1;

                Set<String> tags = getTags(cvConfiguration.getUuid());
                List<MLExperiments> experiments = get24x7Experiments(MLAnalysisType.TIME_SERIES.name());
                if (isEmpty(tags)) {
                  LearningEngineAnalysisTask learningEngineAnalysisTask = createLearningEngineAnalysisTask(
                      accountId, cvConfiguration, analysisStartMinute, endMinute, null);

                  if (learningEngineAnalysisTask != null) {
                    learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);

                    logger.info("Triggering Data Analysis for account {} ", accountId);
                    logger.info("Queuing analysis task for state {} config {} and tag {} with startTime {}",
                        cvConfiguration.getStateType(), cvConfiguration.getUuid(), null, analysisStartMinute);

                    for (MLExperiments experiment : experiments) {
                      LearningEngineExperimentalAnalysisTask task =
                          createLearningEngineAnalysisExperimentalTask(accountId, cvConfiguration, analysisStartMinute,
                              endMinute, null, experiment.getExperimentName());
                      learningEngineService.addLearningEngineExperimentalAnalysisTask(task);
                    }
                  }
                }
                timeSeriesLETask(tags, accountId, cvConfiguration, analysisStartMinute, endMinute, experiments);
              }
            } catch (Exception ex) {
              logger.error("Exception occurred while triggering metric data collection for cvConfig {}",
                  cvConfiguration.getUuid(), ex);
            }
          }
        });
  }

  private void timeSeriesLETask(Set<String> tags, String accountId, CVConfiguration cvConfiguration,
      long analysisStartMinute, long endMinute, List<MLExperiments> experiments) {
    for (String tag : tags) {
      LearningEngineAnalysisTask learningEngineAnalysisTask =
          createLearningEngineAnalysisTask(accountId, cvConfiguration, analysisStartMinute, endMinute, tag);
      if (learningEngineAnalysisTask != null) {
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
  }

  private Set<String> getTags(String cvConfigId) {
    Set<String> tags = new HashSet<>();
    TimeSeriesMetricTemplates template = wingsPersistence.createQuery(TimeSeriesMetricTemplates.class, excludeAuthority)
                                             .filter(TimeSeriesMetricTemplatesKeys.cvConfigId, cvConfigId)
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

  private List<MLExperiments> get24x7Experiments(String analysisType) {
    return wingsPersistence.createQuery(MLExperiments.class, excludeAuthority)
        .filter("is24x7", true)
        .filter(MLExperimentsKeys.ml_analysis_type, analysisType)
        .asList();
  }

  private LearningEngineAnalysisTask createLearningEngineAnalysisTask(
      String accountId, CVConfiguration cvConfiguration, long startMin, long endMin, String tag) {
    String learningTaskId = generateUuid();

    String testInputUrl = getDataFetchUrl(cvConfiguration, startMin - PREDECTIVE_HISTORY_MINUTES, endMin, tag);
    String metricAnalysisSaveUrl = getMetricAnalysisSaveUrl(cvConfiguration, endMin, learningTaskId, tag);
    String historicalAnalysisUrl = getHistoricalAnalysisUrl(cvConfiguration, endMin, tag);
    String failureUrl = "/verification/" + LearningEngineService.RESOURCE_URL
        + VerificationConstants.NOTIFY_LEARNING_FAILURE + "?taskId=" + learningTaskId;

    String metricTemplateUrl = getMetricTemplateUrl(accountId, cvConfiguration.getAppId(),
        cvConfiguration.getStateType(), cvConfiguration.getServiceId(), cvConfiguration.getUuid());

    final String stateExecutionIdForLETask = CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid();

    // clear up any old failed task with the same ID and time.
    learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) endMin);

    if (!learningEngineService.isEligibleToCreateTask(
            stateExecutionIdForLETask, cvConfiguration.getUuid(), endMin, MLAnalysisType.TIME_SERIES)) {
      return null;
    }
    int nextBackoffCount = learningEngineService.getNextServiceGuardBackoffCount(
        stateExecutionIdForLETask, cvConfiguration.getUuid(), endMin, MLAnalysisType.TIME_SERIES);

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
            .analysis_failure_url(failureUrl)
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
            .service_guard_backoff_count(nextBackoffCount)
            .tag(tag)
            .alertThreshold(getAlertThreshold(cvConfiguration, endMin))
            .keyTransactionsUrl(getKeyTransactionsUrl(cvConfiguration))
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

    final String stateExecutionIdForLETask =
        CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid() + "_" + endMin + "-" + tag;

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
            .analysis_save_url(getSaveUrlForExperimentalTask(learningTaskId))
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
            .alertThreshold(getAlertThreshold(cvConfiguration, endMin))
            .keyTransactionsUrl(getKeyTransactionsUrl(cvConfiguration))
            .build();
    learningEngineAnalysisTask.setAppId(cvConfiguration.getAppId());
    learningEngineAnalysisTask.setUuid(learningTaskId);

    return learningEngineAnalysisTask;
  }

  private String getSaveUrlForExperimentalTask(String taskId) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/save-dummy-experimental-247");
    uriBuilder.addParameter("cvConfigId", "Dummy");
    uriBuilder.addParameter("taskId", taskId);
    return getUriString(uriBuilder);
  }

  private String getMetricTemplateUrl(
      String accountId, String appId, StateType stateType, String serviceId, String cvConfigId) {
    final String stateExecutionIdForLETask = CV_24x7_STATE_EXECUTION + "-" + cvConfigId;
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/get-metric-template");
    uriBuilder.addParameter("accountId", accountId);
    uriBuilder.addParameter("appId", appId);
    uriBuilder.addParameter("stateType", stateType.toString());
    uriBuilder.addParameter("cvConfigId", cvConfigId);
    uriBuilder.addParameter("stateExecutionId", stateExecutionIdForLETask);
    return getUriString(uriBuilder);
  }

  private String getMetricAnalysisSaveUrl(CVConfiguration cvConfiguration, long endMinute, String taskId, String tag) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/save-analysis");
    uriBuilder.addParameter("accountId", cvConfiguration.getAccountId());
    uriBuilder.addParameter("applicationId", cvConfiguration.getAppId());
    uriBuilder.addParameter("stateType", cvConfiguration.getStateType().toString());
    uriBuilder.addParameter("serviceId", cvConfiguration.getServiceId());
    uriBuilder.addParameter("cvConfigId", cvConfiguration.getUuid());
    uriBuilder.addParameter("analysisMinute", String.valueOf(endMinute));
    uriBuilder.addParameter("taskId", taskId);
    if (tag != null) {
      uriBuilder.addParameter("tag", tag);
    }
    return getUriString(uriBuilder);
  }
  private String getUriString(URIBuilder uriBuilder) {
    try {
      return uriBuilder.build().toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private String getDataFetchUrl(CVConfiguration cvConfiguration, long startMinute, long endMinute, String tag) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/get-metric-data-247");
    uriBuilder.addParameter("accountId", cvConfiguration.getAccountId());
    uriBuilder.addParameter("appId", cvConfiguration.getAppId());
    uriBuilder.addParameter("stateType", cvConfiguration.getStateType().toString());
    uriBuilder.addParameter("serviceId", cvConfiguration.getServiceId());
    uriBuilder.addParameter("cvConfigId", cvConfiguration.getUuid());
    uriBuilder.addParameter("analysisStartMin", String.valueOf(startMinute));
    uriBuilder.addParameter("analysisEndMin", String.valueOf(endMinute));
    if (tag != null) {
      uriBuilder.addParameter("tag", tag);
    }
    return getUriString(uriBuilder);
  }

  private String getPreviousAnalysisUrl(CVConfiguration cvConfiguration, String tag) {
    long min = timeSeriesAnalysisService.getLastCVAnalysisMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/previous-analysis-247");
    uriBuilder.addParameter("appId", cvConfiguration.getAppId());
    uriBuilder.addParameter("cvConfigId", cvConfiguration.getUuid());
    uriBuilder.addParameter("dataCollectionMin", String.valueOf(min));
    if (tag != null) {
      uriBuilder.addParameter("tag", tag);
    }
    return getUriString(uriBuilder);
  }

  private String getHistoricalAnalysisUrl(CVConfiguration cvConfiguration, long minute, String tag) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/historical-analysis-24x7");
    uriBuilder.addParameter("accountId", cvConfiguration.getAccountId());
    uriBuilder.addParameter("applicationId", cvConfiguration.getAppId());
    uriBuilder.addParameter("serviceId", cvConfiguration.getServiceId());
    uriBuilder.addParameter("analysisMinute", String.valueOf(minute));
    uriBuilder.addParameter("cvConfigId", cvConfiguration.getUuid());
    if (tag != null) {
      uriBuilder.addParameter("tag", tag);
    }
    return getUriString(uriBuilder);
  }

  private String getPreviousAnomaliesUrl(CVConfiguration cvConfiguration, String tag) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/previous-anomalies-247");
    uriBuilder.addParameter("accountId", cvConfiguration.getAccountId());
    uriBuilder.addParameter("applicationId", cvConfiguration.getAppId());
    uriBuilder.addParameter("cvConfigId", cvConfiguration.getUuid());
    if (tag != null) {
      uriBuilder.addParameter("tag", tag);
    }
    return getUriString(uriBuilder);
  }

  private String getCumulativeSumsUrl(CVConfiguration cvConfiguration, int analysisMinute, String tag) {
    int startMin = analysisMinute - (int) TimeUnit.DAYS.toMinutes(1);
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/cumulative-sums-247");
    uriBuilder.addParameter("accountId", cvConfiguration.getAccountId());
    uriBuilder.addParameter("applicationId", cvConfiguration.getAppId());
    uriBuilder.addParameter("cvConfigId", cvConfiguration.getUuid());
    uriBuilder.addParameter("analysisMinStart", String.valueOf(startMin));
    uriBuilder.addParameter("analysisMinEnd", String.valueOf(analysisMinute));
    if (tag != null) {
      uriBuilder.addParameter("tag", tag);
    }
    return getUriString(uriBuilder);
  }

  private String getKeyTransactionsUrl(CVConfiguration cvConfiguration) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(
        VERIFICATION_SERVICE_BASE_URL + "/" + MetricDataAnalysisService.RESOURCE_URL + "/key-transactions-247");
    uriBuilder.addParameter("cvConfigId", cvConfiguration.getUuid());
    return getUriString(uriBuilder);
  }

  private long getCollectionStartTimeForLogs(LogsCVConfiguration logsCVConfiguration, long maxCvCollectionMinute) {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    if (maxCvCollectionMinute <= 0) {
      logger.info("For {} there has been no data collected yet. So starting with baselineStart: {}",
          logsCVConfiguration.getUuid(), logsCVConfiguration.getBaselineStartMinute());
      return logsCVConfiguration.getBaselineStartMinute();
    } else if (maxCvCollectionMinute == logsCVConfiguration.getBaselineEndMinute()) {
      // if baselineEnd is within the past 2 hours, then just continue to nextMinute. Else start from 2 hours ago.
      if (!isBeforeTwoHours(logsCVConfiguration.getBaselineEndMinute())) {
        logger.info("For {} baselineEnd was within the past 2 hours, continuing to the next minute {}",
            logsCVConfiguration.getUuid(), maxCvCollectionMinute + 1);
        return maxCvCollectionMinute + 1;
      } else {
        long expectedStart = getFlooredStartTime(currentMinute, PREDECTIVE_HISTORY_MINUTES);
        logger.info(
            "For {} baselineEnd was more than 2 hours ago, we will start the collection from 2hours ago now: {}",
            logsCVConfiguration.getUuid(), expectedStart);
        return expectedStart;
      }
    } else {
      // 2 cases.
      if (!isBeforeTwoHours(maxCvCollectionMinute)) {
        logger.info("All is as expected. For {}, the collection start time is going to be {}",
            logsCVConfiguration.getUuid(), maxCvCollectionMinute + 1);
        return maxCvCollectionMinute + 1;
      } else {
        long expectedStart = getFlooredStartTime(currentMinute, PREDECTIVE_HISTORY_MINUTES);
        logger.info(
            "It has been more than 2 hours since last collection. For {}, the collection start time is going to be {}",
            logsCVConfiguration.getUuid(), expectedStart);
        return expectedStart;
      }
    }
  }

  private long getFlooredStartTime(long currentTime, long delta) {
    long expectedStart = currentTime - delta;
    if (Math.floorMod(expectedStart - 1, CRON_POLL_INTERVAL_IN_MINUTES) != 0) {
      expectedStart -= Math.floorMod(expectedStart - 1, CRON_POLL_INTERVAL_IN_MINUTES);
    }
    return expectedStart;
  }

  private long getFlooredEndTime(long currentTime, long delta) {
    long expectedStart = currentTime - delta;
    if (Math.floorMod(expectedStart, CRON_POLL_INTERVAL_IN_MINUTES) != 0) {
      expectedStart -= Math.floorMod(expectedStart, CRON_POLL_INTERVAL_IN_MINUTES);
    }
    return expectedStart;
  }

  private boolean isBeforeTwoHours(long minuteToCheck) {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    return minuteToCheck + PREDECTIVE_HISTORY_MINUTES < currentMinute;
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

            long startTime =
                TimeUnit.MINUTES.toMillis(getCollectionStartTimeForLogs(logsCVConfiguration, maxCVCollectionMinute));
            long endTime = startTime + TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES - 1);

            if (PREDICTIVE == cvConfiguration.getComparisonStrategy()
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
              if (isCVTaskBasedCollectionEnabled(cvConfiguration)) {
                createCVTask(cvConfiguration, startTime, endTime);
              } else {
                logger.info(
                    "triggering data collection for state {} config {} startTime {} endTime {} collectionMinute {}",
                    cvConfiguration.getStateType(), cvConfiguration.getUuid(), startTime, endTime, endMinute);
                verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerCVDataCollection(
                    cvConfiguration.getUuid(), cvConfiguration.getStateType(), startTime, endTime));
              }
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

  private boolean isCVTaskBasedCollectionEnabled(CVConfiguration cvConfiguration) {
    boolean isEnabled = false;
    if (cvConfiguration instanceof SplunkCVConfiguration) {
      isEnabled = true;
    } else if (cvConfiguration instanceof NewRelicCVServiceConfiguration) {
      isEnabled = isFeatureFlagEnabled(FeatureName.NEWRELIC_24_7_CV_TASK, cvConfiguration.getAccountId());
    } else if (cvConfiguration instanceof ElkCVConfiguration) {
      isEnabled = isFeatureFlagEnabled(FeatureName.ELK_24_7_CV_TASK, cvConfiguration.getAccountId());
    }
    // TODO: add here once new provider is added. This is only needed till we completely migrate to the new framework.
    return isEnabled;
  }

  private void createCVTask(CVConfiguration cvConfiguration, long startTime, long endTime) {
    DataCollectionInfoV2 dataCollectionInfo = cvConfiguration.toDataCollectionInfo();
    dataCollectionInfo.setStartTime(Instant.ofEpochMilli(startTime));
    dataCollectionInfo.setEndTime(Instant.ofEpochMilli(endTime));
    CVTask cvTask = CVTask.builder()
                        .status(ExecutionStatus.QUEUED)
                        .cvConfigId(cvConfiguration.getUuid())
                        .accountId(cvConfiguration.getAccountId())
                        .dataCollectionInfo(dataCollectionInfo)
                        .build();
    cvTaskService.saveCVTask(cvTask);
  }

  @Override
  @Counted
  @Timed
  public boolean triggerWorkflowDataCollection(AnalysisContext context) {
    long lastDataCollectionMinute;
    if (context.getAnalysisType() == MLAnalysisType.TIME_SERIES) {
      lastDataCollectionMinute = timeSeriesAnalysisService.getLastDataCollectedMinute(
          context.getAppId(), context.getStateExecutionId(), context.getStateType());
    } else {
      lastDataCollectionMinute = logAnalysisService.getLastLogDataCollectedMinute(
          context.getQuery(), context.getAppId(), context.getStateExecutionId(), context.getStateType());
    }
    logger.info("Inside triggerWorkflowCollection with stateType {}, stateExecutionId {} lastDataCollectionMinute {}",
        context.getStateType(), context.getStateExecutionId(), lastDataCollectionMinute);
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
            logger.info("Trigger Data Collection with stateType {}, stateExecutionId {} lastDataCollectionMinute {}",
                context.getStateType(), context.getStateExecutionId(), lastDataCollectionMinute);
            return verificationManagerClientHelper
                .callManagerWithRetry(verificationManagerClient.triggerWorkflowDataCollection(
                    context.getUuid(), lastDataCollectionMinute + 1))
                .getResource();
          } else {
            logger.info("Completed Data Collection for stateType {}, stateExecutionId {}", context.getStateType(),
                context.getStateExecutionId());
            return false;
          }
        }
      } catch (Exception e) {
        logger.error(
            "Failed to call manager for data collection for workflow with context {} with exception {}", context, e);
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
                && cvConfiguration.getStateType() != StateType.SPLUNKV2)
        .forEach(cvConfiguration -> {
          try (VerificationLogContext ignored = new VerificationLogContext(cvConfiguration.getAccountId(),
                   cvConfiguration.getUuid(), null, cvConfiguration.getStateType(), OVERRIDE_ERROR)) {
            long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
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
            long maxLogRecordMinute = logAnalysisService.getLogRecordMinute(
                cvConfiguration.getAppId(), cvConfiguration.getUuid(), ClusterLevel.H0, OrderType.DESC);

            if (isBeforeTwoHours(lastCVDataCollectionMinute) || isBeforeTwoHours(maxLogRecordMinute)) {
              logger.info(
                  "For account {} and CV config {} name {} type {} There has been no new data in the past 2 hours. Skipping L1 clustering",
                  cvConfiguration.getAccountId(), cvConfiguration.getUuid(), cvConfiguration.getName(),
                  cvConfiguration.getStateType());
              return;
            }

            if (isBeforeTwoHours(minLogRecordMinute)) {
              logger.info(
                  "for {} minLogRecordMinute is more than 2 hours ago but maxLogRecordMinute is less than 2 hours ago. We will start L1 clustering from 2 hours ago.",
                  cvConfiguration.getUuid());
              minLogRecordMinute = currentMinute - PREDECTIVE_HISTORY_MINUTES;
            }

            logger.info("Clustering pending between {} and {}", minLogRecordMinute, lastCVDataCollectionMinute);

            for (long logRecordMinute = minLogRecordMinute;
                 logRecordMinute > 0 && logRecordMinute <= lastCVDataCollectionMinute; logRecordMinute++) {
              Set<String> hosts = logAnalysisService.getHostsForMinute(cvConfiguration.getAppId(),
                  LogDataRecordKeys.cvConfigId, cvConfiguration.getUuid(), logRecordMinute, ClusterLevel.L0);

              // there can be a race between finding all the host for a min and le finishing the cluster task and
              // deleting L0 data
              if (isEmpty(hosts)) {
                logger.info("For {} minute {} did not find hosts for level {} continuing...", cvConfiguration.getUuid(),
                    logRecordMinute, ClusterLevel.L0);
                logAnalysisService.saveClusteredLogData(cvConfiguration.getAppId(), cvConfiguration.getUuid(),
                    ClusterLevel.L1, (int) logRecordMinute, null, Lists.newArrayList());
                continue;
              }
              final String taskId = generateUuid();
              String inputLogsUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_GET_24X7_LOG_URL + "?cvConfigId=" + cvConfiguration.getUuid()
                  + "&appId=" + cvConfiguration.getAppId() + "&clusterLevel=" + ClusterLevel.L0
                  + "&logCollectionMinute=" + logRecordMinute;
              String clusteredLogSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL
                  + "?cvConfigId=" + cvConfiguration.getUuid() + "&appId=" + cvConfiguration.getAppId()
                  + "&clusterLevel=" + ClusterLevel.L1 + "&logCollectionMinute=" + logRecordMinute;
              String failureUrl = "/verification/" + LearningEngineService.RESOURCE_URL
                  + VerificationConstants.NOTIFY_LEARNING_FAILURE + "?taskId=" + taskId;

              String stateExecutionIdForLETask = "LOGS_CLUSTER_L1_" + cvConfiguration.getUuid() + "_" + logRecordMinute;
              learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) logRecordMinute);

              if (learningEngineService.isEligibleToCreateTask(stateExecutionIdForLETask, cvConfiguration.getUuid(),
                      logRecordMinute, MLAnalysisType.LOG_CLUSTER)) {
                int nextBackoffCount = learningEngineService.getNextServiceGuardBackoffCount(
                    stateExecutionIdForLETask, cvConfiguration.getUuid(), logRecordMinute, MLAnalysisType.LOG_CLUSTER);
                LearningEngineAnalysisTask analysisTask =
                    LearningEngineAnalysisTask.builder()
                        .control_input_url(inputLogsUrl)
                        .analysis_save_url(clusteredLogSaveUrl)
                        .analysis_failure_url(failureUrl)
                        .state_execution_id(stateExecutionIdForLETask)
                        .service_id(cvConfiguration.getServiceId())
                        .control_nodes(hosts)
                        .sim_threshold(0.99)
                        .service_guard_backoff_count(nextBackoffCount)
                        .analysis_minute(logRecordMinute)
                        .cluster_level(ClusterLevel.L1.getLevel())
                        .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                        .stateType(cvConfiguration.getStateType())
                        .query(Lists.newArrayList(((LogsCVConfiguration) cvConfiguration).getQuery()))
                        .is24x7Task(true)
                        .cvConfigId(cvConfiguration.getUuid())
                        .alertThreshold(getAlertThreshold(cvConfiguration, logRecordMinute))
                        .build();
                analysisTask.setAppId(cvConfiguration.getAppId());
                analysisTask.setUuid(taskId);

                learningEngineService.addLearningEngineAnalysisTask(analysisTask);

                List<MLExperiments> experiments = get24x7Experiments(MLAnalysisType.LOG_CLUSTER.name());
                for (MLExperiments experiment : experiments) {
                  LearningEngineExperimentalAnalysisTask expTask =
                      LearningEngineExperimentalAnalysisTask.builder()
                          .control_input_url(inputLogsUrl)
                          .analysis_save_url(getSaveUrlForExperimentalTask(taskId))
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
                          .alertThreshold(getAlertThreshold(cvConfiguration, logRecordMinute))
                          .build();
                  expTask.setAppId(cvConfiguration.getAppId());
                  learningEngineService.addLearningEngineExperimentalAnalysisTask(expTask);
                }
              }
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
                && cvConfiguration.getStateType() != StateType.SPLUNKV2)
        .forEach(cvConfiguration -> {
          try (VerificationLogContext ignored = new VerificationLogContext(cvConfiguration.getAccountId(),
                   cvConfiguration.getUuid(), null, cvConfiguration.getStateType(), OVERRIDE_ERROR)) {
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

              long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
              if (isBeforeTwoHours(maxLogRecordL1Minute)) {
                logger.info(
                    "For account {} and CV config {} name {} type {} There has been no new L1 clusters in the past 2 hours. Skipping L1 to L2 clustering",
                    cvConfiguration.getAccountId(), cvConfiguration.getUuid(), cvConfiguration.getName(),
                    cvConfiguration.getStateType());
                return;
              }
              if (isBeforeTwoHours(minLogRecordL1Minute)) {
                logger.info(
                    "for {} minLogRecordMinute is more than 2 hours ago but maxLogRecordMinute is less than 2 hours ago. We will start L1 clustering from 2 hours ago.",
                    cvConfiguration.getUuid());
                minLogRecordL1Minute = currentMinute - PREDECTIVE_HISTORY_MINUTES;
              }

              if (AnalysisComparisonStrategy.PREDICTIVE != cvConfiguration.getComparisonStrategy()
                  && maxLogRecordL1Minute < minLogRecordL1Minute + CRON_POLL_INTERVAL_IN_MINUTES - 1) {
                logger.info(
                    "For CV config {} there is still node data clustering is pending. min l1 {} max l1 {}. Skipping L2 clustering",
                    cvConfiguration.getUuid(), minLogRecordL1Minute, maxLogRecordL1Minute);
                return;
              }
              maxLogRecordL1Minute = minLogRecordL1Minute + CRON_POLL_INTERVAL_IN_MINUTES - 1;

              if (PREDICTIVE == cvConfiguration.getComparisonStrategy()
                  && minLogRecordL1Minute >= ((LogsCVConfiguration) cvConfiguration).getBaselineEndMinute()) {
                maxLogRecordL1Minute = minLogRecordL1Minute + 1;
              }

              for (long logRecordMinute = minLogRecordL1Minute; logRecordMinute < maxLogRecordL1Minute;
                   logRecordMinute++) {
                Set<String> hosts =
                    logAnalysisService.getHostsForMinute(cvConfiguration.getAppId(), LogDataRecordKeys.cvConfigId,
                        cvConfiguration.getUuid(), logRecordMinute, ClusterLevel.L0, ClusterLevel.H0);
                if (isNotEmpty(hosts)) {
                  logger.info(
                      "For CV config {} there is still node data clustering is pending for {} for minute {}. Skipping L2 clustering",
                      cvConfiguration.getUuid(), hosts, logRecordMinute);
                  return;
                }

                hosts = logAnalysisService.getHostsForMinute(cvConfiguration.getAppId(), LogDataRecordKeys.cvConfigId,
                    cvConfiguration.getUuid(), logRecordMinute, ClusterLevel.L1, ClusterLevel.H1);
                if (isEmpty(hosts)) {
                  logger.info(
                      "For CV config {} there is no clustering data present for minute {}. Skipping L2 clustering",
                      cvConfiguration.getUuid(), logRecordMinute);
                  return;
                }
              }
              logger.info("for {} for minute from {} to {} everything is in place, proceeding for L2 Clustering",
                  cvConfiguration.getUuid(), minLogRecordL1Minute, maxLogRecordL1Minute);

              final String taskId = generateUuid();
              String inputLogsUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL + "?cvConfigId=" + cvConfiguration.getUuid()
                  + "&appId=" + cvConfiguration.getAppId() + "&clusterLevel=" + ClusterLevel.L1
                  + "&startMinute=" + minLogRecordL1Minute + "&endMinute=" + maxLogRecordL1Minute;
              String clusteredLogSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_STATE_SAVE_24X7_CLUSTERED_LOG_URL
                  + "?cvConfigId=" + cvConfiguration.getUuid() + "&appId=" + cvConfiguration.getAppId()
                  + "&clusterLevel=" + ClusterLevel.L2 + "&logCollectionMinute=" + maxLogRecordL1Minute;

              String failureUrl = "/verification/" + LearningEngineService.RESOURCE_URL
                  + VerificationConstants.NOTIFY_LEARNING_FAILURE + "?taskId=" + taskId;
              String stateExecutionIdForLETask =
                  "LOGS_CLUSTER_L2_" + cvConfiguration.getUuid() + "_" + maxLogRecordL1Minute;
              learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) maxLogRecordL1Minute);

              if (learningEngineService.isEligibleToCreateTask(stateExecutionIdForLETask, cvConfiguration.getUuid(),
                      maxLogRecordL1Minute, MLAnalysisType.LOG_CLUSTER)) {
                int nextBackoffCount = learningEngineService.getNextServiceGuardBackoffCount(stateExecutionIdForLETask,
                    cvConfiguration.getUuid(), maxLogRecordL1Minute, MLAnalysisType.LOG_CLUSTER);
                LearningEngineAnalysisTask analysisTask =
                    LearningEngineAnalysisTask.builder()
                        .control_input_url(inputLogsUrl)
                        .analysis_save_url(clusteredLogSaveUrl)
                        .analysis_failure_url(failureUrl)
                        .state_execution_id(stateExecutionIdForLETask)
                        .service_id(cvConfiguration.getServiceId())
                        .control_nodes(Collections.emptySet())
                        .sim_threshold(0.99)
                        .service_guard_backoff_count(nextBackoffCount)
                        .analysis_minute(maxLogRecordL1Minute)
                        .cluster_level(ClusterLevel.L2.getLevel())
                        .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                        .stateType(cvConfiguration.getStateType())
                        .query(Lists.newArrayList(((LogsCVConfiguration) cvConfiguration).getQuery()))
                        .is24x7Task(true)
                        .cvConfigId(cvConfiguration.getUuid())
                        .alertThreshold(getAlertThreshold(cvConfiguration, maxLogRecordL1Minute))
                        .build();
                analysisTask.setAppId(cvConfiguration.getAppId());
                analysisTask.setUuid(taskId);

                final boolean taskQueued = learningEngineService.addLearningEngineAnalysisTask(analysisTask);
                if (taskQueued) {
                  logger.info("L2 Clustering queued for cvConfig {} from minute {} to minute {}",
                      cvConfiguration.getUuid(), minLogRecordL1Minute, maxLogRecordL1Minute);
                }
                List<MLExperiments> experiments = get24x7Experiments(MLAnalysisType.LOG_CLUSTER.name());
                for (MLExperiments experiment : experiments) {
                  LearningEngineExperimentalAnalysisTask expTask =
                      LearningEngineExperimentalAnalysisTask.builder()
                          .control_input_url(inputLogsUrl)
                          .analysis_save_url(getSaveUrlForExperimentalTask(taskId))
                          .state_execution_id("LOGS_CLUSTER_L2_" + cvConfiguration.getUuid() + "_"
                              + maxLogRecordL1Minute + "-" + generateUUID())
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
                          .alertThreshold(getAlertThreshold(cvConfiguration, maxLogRecordL1Minute))
                          .build();
                  expTask.setAppId(cvConfiguration.getAppId());
                  learningEngineService.addLearningEngineExperimentalAnalysisTask(expTask);
                }
              }
            } catch (Exception ex) {
              logger.error("Creating L2 task failed for cvConfig " + cvConfiguration.getUuid());
            }
          }
        });
  }

  private long getFeedbackAnalysisMinute(LogsCVConfiguration logsCVConfiguration) {
    long lastFeedbackAnalysisMinute = logAnalysisService.getLastCVAnalysisMinute(
        logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid(), LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);
    long minuteForFeedbackAnalysis = lastFeedbackAnalysisMinute + CRON_POLL_INTERVAL_IN_MINUTES;
    long lastLogMLAnalysisMinute = logAnalysisService.getLastCVAnalysisMinute(
        logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid(), LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);
    if (isBeforeTwoHours(minuteForFeedbackAnalysis)) {
      if (isBeforeTwoHours(lastLogMLAnalysisMinute)) {
        logger.info(
            "The last LogML analysis was also more than 2 hours ago, we dont have anything to do for feedback tasks.");
        return -1;
      }

      while (isBeforeTwoHours(minuteForFeedbackAnalysis)) {
        minuteForFeedbackAnalysis += CRON_POLL_INTERVAL_IN_MINUTES;
      }
    }

    if (lastFeedbackAnalysisMinute <= 0
        || !hasBaselineAnalysisStartedForFeedback(logsCVConfiguration, lastFeedbackAnalysisMinute)) {
      if (lastLogMLAnalysisMinute <= 0) {
        logger.info(
            "For account {} and CV config {} name {} type {} no LogML analysis has happened yet. Skipping feedback analysis",
            logsCVConfiguration.getAccountId(), logsCVConfiguration.getUuid(), logsCVConfiguration.getName(),
            logsCVConfiguration.getStateType());
        return -1;
      } else {
        minuteForFeedbackAnalysis = lastLogMLAnalysisMinute;
      }
    } else if (minuteForFeedbackAnalysis > lastLogMLAnalysisMinute) {
      logger.info("It is not time for the next feedback analysis yet. We'll wait for some time.");
      return -1;
    }

    return minuteForFeedbackAnalysis;
  }

  private boolean hasBaselineAnalysisStartedForFeedback(
      LogsCVConfiguration cvConfiguration, long lastFeedbackAnalysisMinute) {
    boolean hasBaselineAnalysisStarted = false;
    if (lastFeedbackAnalysisMinute >= cvConfiguration.getBaselineStartMinute()) {
      return true;
    }
    return false;
  }

  @Override
  @Counted
  @Timed
  public void triggerFeedbackAnalysis(String accountId) {
    boolean isFlagEnabled = isFeatureFlagEnabled(FeatureName.CV_FEEDBACKS, accountId);
    if (!isFlagEnabled) {
      logger.info(
          "CV Feedbacks feature flag is not enabled for account {}, not going to create a feedback task", accountId);
      return;
    }
    // List all the CV configurations for a given account
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);

    cvConfigurations.stream()
        .filter(cvConfiguration
            -> cvConfiguration.isEnabled24x7() && getLogAnalysisStates().contains(cvConfiguration.getStateType()))

        .forEach(cvConfiguration -> {
          LogsCVConfiguration logsCVConfiguration = (LogsCVConfiguration) cvConfiguration;
          try (VerificationLogContext ignored = new VerificationLogContext(logsCVConfiguration.getAccountId(),
                   logsCVConfiguration.getUuid(), null, logsCVConfiguration.getStateType(), OVERRIDE_ERROR)) {
            if (!logsCVConfiguration.isWorkflowConfig()) {
              long minuteForFeedbackAnalysis = getFeedbackAnalysisMinute(logsCVConfiguration);

              if (minuteForFeedbackAnalysis <= 0) {
                return;
              }

              long lastLogMLAnalysisMinute = logAnalysisService.getLastCVAnalysisMinute(logsCVConfiguration.getAppId(),
                  logsCVConfiguration.getUuid(), LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);

              AtomicBoolean isEmptyFeedbacks = new AtomicBoolean(true);

              Map<FeedbackAction, List<CVFeedbackRecord>> feedbacks =
                  logAnalysisService.getUserFeedback(cvConfiguration.getUuid(), null, cvConfiguration.getAppId());
              feedbacks.forEach((action, list) -> {
                if (isNotEmpty(list)) {
                  isEmptyFeedbacks.set(false);
                }
              });
              if (isEmptyFeedbacks.get()) {
                logAnalysisService.createAndUpdateFeedbackAnalysis(
                    LogMLAnalysisRecordKeys.cvConfigId, cvConfiguration.getUuid(), lastLogMLAnalysisMinute);
              } else {
                boolean feedbackTask = createFeedbackAnalysisTask(logsCVConfiguration, minuteForFeedbackAnalysis);
                logger.info("Created Feedback analysis task for {} and minute {}", logsCVConfiguration.getUuid(),
                    minuteForFeedbackAnalysis);
                if (feedbackTask) {
                  createExperimentalFeedbackTask(logsCVConfiguration, minuteForFeedbackAnalysis);
                }
              }
            }
          }
        });
  }

  private boolean isFeatureFlagEnabled(FeatureName featureName, String accountId) {
    return verificationManagerClientHelper
        .callManagerWithRetry(verificationManagerClient.isFeatureEnabled(featureName, accountId))
        .getResource();
  }
  private void createExperimentalFeedbackTask(LogsCVConfiguration logsCVConfiguration, long logCollectionMinute) {
    try {
      String stateExecutionIdForLETask =
          "LOG_24X7_EXP_FEEDBACK_ANALYSIS_" + logsCVConfiguration.getUuid() + "_" + logCollectionMinute;
      logger.info("Creating Experimental Feedback analysis task for {} and minute {}", logsCVConfiguration.getUuid(),
          logCollectionMinute);
      String feedbackUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS + GET_LOG_FEEDBACKS
          + "?cvConfigId=" + logsCVConfiguration.getUuid() + "&appId=" + logsCVConfiguration.getAppId();
      final String logMLResultUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
          + LogAnalysisResource.ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL + "?appId=" + logsCVConfiguration.getAppId()
          + "&cvConfigId=" + logsCVConfiguration.getUuid() + "&analysisMinute=" + logCollectionMinute;
      final String taskId = generateUuid();
      final String logAnalysisSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
          + LogAnalysisResource.ANALYSIS_SAVE_EXP_24X7_ANALYSIS_RECORDS_URL
          + "?cvConfigId=" + logsCVConfiguration.getUuid() + "&appId=" + logsCVConfiguration.getAppId()
          + "&analysisMinute=" + logCollectionMinute + "&taskId=" + taskId + "&isFeedbackAnalysis=true";
      List<MLExperiments> experiments = get24x7Experiments(MLAnalysisType.FEEDBACK_ANALYSIS.name());
      for (MLExperiments experiment : experiments) {
        LearningEngineExperimentalAnalysisTask expTask =
            LearningEngineExperimentalAnalysisTask.builder()
                .feedback_url(feedbackUrl)
                .logMLResultUrl(logMLResultUrl)
                .state_execution_id(stateExecutionIdForLETask)
                .query(Arrays.asList(logsCVConfiguration.getQuery()))
                .ml_analysis_type(MLAnalysisType.FEEDBACK_ANALYSIS)
                .analysis_save_url(logAnalysisSaveUrl)
                .cvConfigId(logsCVConfiguration.getUuid())
                .service_id(logsCVConfiguration.getServiceId())
                //        .shouldUseSupervisedModel(learningEngineService.shouldUseSupervisedModel(
                //          SupervisedTrainingStatusKeys.serviceId, logsCVConfiguration.getServiceId()))
                .analysis_minute(logCollectionMinute)
                .stateType(logsCVConfiguration.getStateType())
                .analysis_comparison_strategy(logsCVConfiguration.getComparisonStrategy())
                .shouldUseSupervisedModel(true)
                .experiment_name(experiment.getExperimentName())
                .build();

        expTask.setAppId(logsCVConfiguration.getAppId());
        expTask.setUuid(taskId);

        learningEngineService.addLearningEngineExperimentalAnalysisTask(expTask);
      }
    } catch (Exception ex) {
      logger.info("Exception while creating experimental feedback task", ex);
    }
  }
  private boolean createFeedbackAnalysisTask(LogsCVConfiguration logsCVConfiguration, long logCollectionMinute) {
    String stateExecutionIdForLETask =
        "LOG_24X7_FEEDBACK_ANALYSIS_" + logsCVConfiguration.getUuid() + "_" + logCollectionMinute;
    logger.info(
        "Creating Feedback analysis task for {} and minute {}", logsCVConfiguration.getUuid(), logCollectionMinute);
    String feedbackUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS + GET_LOG_FEEDBACKS
        + "?cvConfigId=" + logsCVConfiguration.getUuid() + "&appId=" + logsCVConfiguration.getAppId();
    final String taskId = generateUuid();
    final String logAnalysisSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL + "?cvConfigId=" + logsCVConfiguration.getUuid()
        + "&appId=" + logsCVConfiguration.getAppId() + "&analysisMinute=" + logCollectionMinute + "&taskId=" + taskId
        + "&isFeedbackAnalysis=true";
    final String logMLResultUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
        + LogAnalysisResource.ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL + "?appId=" + logsCVConfiguration.getAppId()
        + "&cvConfigId=" + logsCVConfiguration.getUuid() + "&analysisMinute=" + logCollectionMinute;
    final String failureUrl = "/verification/" + LearningEngineService.RESOURCE_URL
        + VerificationConstants.NOTIFY_LEARNING_FAILURE + "?taskId=" + taskId;
    LearningEngineAnalysisTask feedbackTask =
        LearningEngineAnalysisTask.builder()
            .feedback_url(feedbackUrl)
            .logMLResultUrl(logMLResultUrl)
            .state_execution_id(stateExecutionIdForLETask)
            .query(Arrays.asList(logsCVConfiguration.getQuery()))
            .ml_analysis_type(MLAnalysisType.FEEDBACK_ANALYSIS)
            .analysis_save_url(logAnalysisSaveUrl)
            .analysis_failure_url(failureUrl)
            .cvConfigId(logsCVConfiguration.getUuid())
            .service_id(logsCVConfiguration.getServiceId())
            .shouldUseSupervisedModel(learningEngineService.shouldUseSupervisedModel(
                SupervisedTrainingStatusKeys.serviceId, logsCVConfiguration.getServiceId()))
            .analysis_minute(logCollectionMinute)
            .stateType(logsCVConfiguration.getStateType())
            .build();

    feedbackTask.setAppId(logsCVConfiguration.getAppId());
    feedbackTask.setUuid(taskId);

    learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) logCollectionMinute);
    if (learningEngineService.isEligibleToCreateTask(stateExecutionIdForLETask, logsCVConfiguration.getUuid(),
            logCollectionMinute, MLAnalysisType.FEEDBACK_ANALYSIS)) {
      int nextBackoffCount = learningEngineService.getNextServiceGuardBackoffCount(stateExecutionIdForLETask,
          logsCVConfiguration.getUuid(), logCollectionMinute, MLAnalysisType.FEEDBACK_ANALYSIS);
      feedbackTask.setService_guard_backoff_count(nextBackoffCount);
      learningEngineService.addLearningEngineAnalysisTask(feedbackTask);
    }
    return false;
  }

  private Double getAlertThreshold(CVConfiguration cvConfiguration, long analysisMin) {
    Double alertThreshold = null;
    if (cvConfiguration.isAlertEnabled()
        && !(analysisMin <= TimeUnit.MILLISECONDS.toMinutes(cvConfiguration.getSnoozeEndTime())
               && analysisMin >= TimeUnit.MILLISECONDS.toMinutes(cvConfiguration.getSnoozeStartTime()))) {
      alertThreshold = cvConfiguration.getAlertThreshold();
    }
    return alertThreshold;
  }

  private long getAnalysisStartMinForLogs(
      LogsCVConfiguration cvConfiguration, long l2RecordMin, long lastAnalysisMinute) {
    long currentMinute = TimeUnit.MILLISECONDS.toMinutes(Timestamp.currentMinuteBoundary());
    if (l2RecordMin <= 0) {
      logger.info("For {}, No L2 has been done so far, returning -1", cvConfiguration.getUuid());
      return -1;
    }
    if (lastAnalysisMinute > l2RecordMin) {
      logger.info("For {}, we still need to wait before doing analysis. Will come back", cvConfiguration.getUuid());
    }
    if (isBeforeTwoHours(l2RecordMin)) {
      // if this is within baseline window, it's cool.
      if (l2RecordMin >= cvConfiguration.getBaselineStartMinute()
          && l2RecordMin <= cvConfiguration.getBaselineEndMinute()) {
        return l2RecordMin;
      } else {
        long l2MaxMin = logAnalysisService.getLogRecordMinute(
            cvConfiguration.getAppId(), cvConfiguration.getUuid(), ClusterLevel.H2, OrderType.DESC);

        if (isBeforeTwoHours(l2MaxMin)) {
          logger.info(
              "For {}, last L2 was more than 2 hours ago, we dont have anything to do now", cvConfiguration.getUuid());
          return -1;
        } else {
          long restartTime = getFlooredStartTime(currentMinute, PREDECTIVE_HISTORY_MINUTES);

          // check to see if there was any task created for this cvConfig for one hour before expected restart time. If
          // yes, dont do this. Return -1
          boolean isTaskRunning = learningEngineService.isTaskRunningOrQueued(
              cvConfiguration.getUuid(), restartTime - PREDECTIVE_HISTORY_MINUTES - 60);
          if (isTaskRunning) {
            return -1;
          }
          logger.info("For {}, we are restarting from 2hours ago. New start time is {}", cvConfiguration.getUuid(),
              restartTime);
          return restartTime;
        }
      }
    }
    return l2RecordMin;
  }

  @Override
  public void trigger247LogDataV2Analysis(LogsCVConfiguration logsCVConfiguration) {
    try {
      long analysisStartMin = logAnalysisService.getLogRecordMinute(
          logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid(), ClusterLevel.H2, OrderType.ASC);
      long lastCVAnalysisMinute = logAnalysisService.getLastCVAnalysisMinute(
          logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid(), LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);

      if (analysisStartMin == -1) {
        logger.info(
            "For account {} and CV config {} name {} type {} no data L2 clustering has happened yet. Skipping 24x7 Log analysis",
            logsCVConfiguration.getAccountId(), logsCVConfiguration.getUuid(), logsCVConfiguration.getName(),
            logsCVConfiguration.getStateType());
        return;
      }

      // Check if anaysis minute is present in last to 2 hours.
      long startMinute = getAnalysisStartMinForLogs(logsCVConfiguration, analysisStartMin, lastCVAnalysisMinute);

      if (startMinute <= 0) {
        logger.info(
            "For account {} and CV config {} name {} type {} There has been no new data in the past 2 hours. Skipping 24x7 Log analysis",
            logsCVConfiguration.getAccountId(), logsCVConfiguration.getUuid(), logsCVConfiguration.getName(),
            logsCVConfiguration.getStateType());
        return;
      }

      long analysisEndMin = startMinute + CRON_POLL_INTERVAL_IN_MINUTES - 1;

      for (long l2Min = analysisStartMin, i = 0; l2Min <= analysisEndMin; l2Min++, i++) {
        Set<String> hosts = logAnalysisService.getHostsForMinute(logsCVConfiguration.getAppId(),
            LogDataRecordKeys.cvConfigId, logsCVConfiguration.getUuid(), l2Min, ClusterLevel.L1, ClusterLevel.H1);
        if (isNotEmpty(hosts)) {
          logger.info(
              "For CV config {} there is still L2 clustering pending for {} for minute {}. Skipping L2 Analysis",
              logsCVConfiguration.getUuid(), hosts, l2Min);
          return;
        }
      }

      logger.info("for {} for minute from {} to {} everything is in place, proceeding for analysis",
          logsCVConfiguration.getUuid(), analysisStartMin, analysisEndMin);

      String taskId = generateUuid();

      String controlInputUrl = null;

      String testInputUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
          + LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL + "?cvConfigId=" + logsCVConfiguration.getUuid()
          + "&appId=" + logsCVConfiguration.getAppId() + "&clusterLevel=" + ClusterLevel.L2
          + "&startMinute=" + analysisStartMin + "&endMinute=" + analysisEndMin;

      String logAnalysisSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
          + LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL + "?cvConfigId=" + logsCVConfiguration.getUuid()
          + "&appId=" + logsCVConfiguration.getAppId() + "&analysisMinute=" + analysisEndMin + "&taskId=" + taskId
          + "&comparisonStrategy=" + logsCVConfiguration.getComparisonStrategy();

      final String logAnalysisGetUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
          + LogAnalysisResource.ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL + "?appId=" + logsCVConfiguration.getAppId()
          + "&cvConfigId=" + logsCVConfiguration.getUuid() + "&analysisMinute=" + lastCVAnalysisMinute + "&compressed="
          + verificationManagerClientHelper
                .callManagerWithRetry(verificationManagerClient.isFeatureEnabled(
                    FeatureName.SEND_LOG_ANALYSIS_COMPRESSED, logsCVConfiguration.getAccountId()))
                .getResource();
      String failureUrl = "/verification/" + LearningEngineService.RESOURCE_URL
          + VerificationConstants.NOTIFY_LEARNING_FAILURE + "?taskId=" + taskId;

      String stateExecutionIdForLETask = "LOG_24X7_V2_ANALYSIS_" + logsCVConfiguration.getUuid() + "_" + analysisEndMin;
      learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) analysisEndMin);

      if (learningEngineService.isEligibleToCreateTask(
              stateExecutionIdForLETask, logsCVConfiguration.getUuid(), analysisEndMin, MLAnalysisType.LOG_ML)) {
        int nextBackoffCount = learningEngineService.getNextServiceGuardBackoffCount(
            stateExecutionIdForLETask, logsCVConfiguration.getUuid(), analysisEndMin, MLAnalysisType.LOG_ML);
        LearningEngineAnalysisTask analysisTask =
            LearningEngineAnalysisTask.builder()
                .state_execution_id(stateExecutionIdForLETask)
                .service_id(logsCVConfiguration.getServiceId())
                .query(Lists.newArrayList(logsCVConfiguration.getQuery()))
                .sim_threshold(0.9)
                .analysis_minute(analysisEndMin)
                .analysis_save_url(logAnalysisSaveUrl)
                .log_analysis_get_url(logAnalysisGetUrl)
                .analysis_failure_url(failureUrl)
                .service_guard_backoff_count(nextBackoffCount)
                .ml_analysis_type(MLAnalysisType.LOG_ML)
                .test_input_url(testInputUrl)
                .test_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
                .feature_name("247_V2")
                .is24x7Task(true)
                .stateType(logsCVConfiguration.getStateType())
                .cvConfigId(logsCVConfiguration.getUuid())
                .analysis_comparison_strategy(logsCVConfiguration.getComparisonStrategy())
                .alertThreshold(getAlertThreshold(logsCVConfiguration, analysisEndMin))
                .build();

        analysisTask.setAppId(logsCVConfiguration.getAppId());
        analysisTask.setUuid(taskId);
        learningEngineService.addLearningEngineAnalysisTask(analysisTask);

        final boolean taskQueued = learningEngineService.addLearningEngineAnalysisTask(analysisTask);
        if (taskQueued) {
          logger.info("24x7 Logs V2 Analysis queued for cvConfig {} for analysis minute {}",
              logsCVConfiguration.getUuid(), analysisEndMin);
        }

        analysisTask.setAppId(logsCVConfiguration.getAppId());
        analysisTask.setUuid(taskId);

        if (logsCVConfiguration.getComparisonStrategy() == PREDICTIVE) {
          final String lastLogAnalysisGetUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
              + LogAnalysisResource.ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL + "?appId=" + logsCVConfiguration.getAppId()
              + "&cvConfigId=" + logsCVConfiguration.getUuid() + "&analysisMinute=" + analysisEndMin;
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

        List<MLExperiments> experiments = get24x7Experiments(MLAnalysisType.LOG_ML.name());
        for (MLExperiments experiment : experiments) {
          LearningEngineExperimentalAnalysisTask expTask =
              LearningEngineExperimentalAnalysisTask.builder()
                  .state_execution_id(
                      "LOG_24X7_V2_ANALYSIS_" + logsCVConfiguration.getUuid() + "_" + analysisEndMin + generateUuid())
                  .service_id(logsCVConfiguration.getServiceId())
                  .query(Lists.newArrayList(logsCVConfiguration.getQuery()))
                  .sim_threshold(0.9)
                  .analysis_minute(analysisEndMin)
                  .analysis_save_url(getSaveUrlForExperimentalTask(taskId))
                  .log_analysis_get_url(logAnalysisGetUrl)
                  .ml_analysis_type(MLAnalysisType.LOG_ML)
                  .test_input_url(isEmpty(testInputUrl) ? null : (testInputUrl + "&" + IS_EXPERIMENTAL + "=true"))
                  .control_input_url(
                      isEmpty(controlInputUrl) ? null : controlInputUrl + "&" + IS_EXPERIMENTAL + "=true")
                  .test_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
                  .feature_name("247_V2")
                  .is24x7Task(true)
                  .stateType(logsCVConfiguration.getStateType())
                  .tolerance(logsCVConfiguration.getAnalysisTolerance().tolerance())
                  .cvConfigId(logsCVConfiguration.getUuid())
                  .analysis_comparison_strategy(logsCVConfiguration.getComparisonStrategy())
                  .experiment_name(experiment.getExperimentName())
                  .alertThreshold(getAlertThreshold(logsCVConfiguration, analysisEndMin))
                  .build();
          expTask.setAppId(logsCVConfiguration.getAppId());
          expTask.setUuid(taskId);
          learningEngineService.addLearningEngineExperimentalAnalysisTask(expTask);
        }
      }
    } catch (Exception ex) {
      logger.error(
          "24x7 V2 Log Data Analysis not successful for Account ID: {} with CVConfigId: {}, name {},  type {}, created at: {}   Exception: {}",
          logsCVConfiguration.getAccountId(), logsCVConfiguration.getUuid(), logsCVConfiguration.getName(),
          logsCVConfiguration.getStateType(), logsCVConfiguration.getCreatedAt(), ex);
    }
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
          /*
           * This is a new 247 service guard log analysis that will be executed
           * in {@link ContinuousVerificationServiceImpl#trigger247LogDataV2Analysis}
           */
          if (logsCVConfiguration.is247LogsV2()) {
            return;
          }

          try (VerificationLogContext ignored = new VerificationLogContext(logsCVConfiguration.getAccountId(),
                   logsCVConfiguration.getUuid(), null, logsCVConfiguration.getStateType(), OVERRIDE_ERROR)) {
            try {
              if (logsCVConfiguration.isWorkflowConfig()) {
                AnalysisContext context =
                    wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
                long analysisLastMin = logAnalysisService.getLogRecordMinute(
                    logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid(), ClusterLevel.HF, OrderType.DESC);
                if (analysisLastMin >= logsCVConfiguration.getBaselineEndMinute() + context.getTimeDuration()) {
                  logger.info("Notifying state id: {} , corr id: {}", context.getStateExecutionId(),
                      context.getCorrelationId());
                  sendStateNotification(context, false, "", (int) analysisLastMin);
                  logger.info("Disabled 24x7 for CV Configuration with id {}", logsCVConfiguration.getUuid());
                  wingsPersistence.updateField(
                      LogsCVConfiguration.class, logsCVConfiguration.getUuid(), "enabled24x7", false);
                  return;
                }
              }
              long analysisStartMin = logAnalysisService.getLogRecordMinute(
                  logsCVConfiguration.getAppId(), logsCVConfiguration.getUuid(), ClusterLevel.H2, OrderType.ASC);
              long lastCVAnalysisMinute = logAnalysisService.getLastCVAnalysisMinute(logsCVConfiguration.getAppId(),
                  logsCVConfiguration.getUuid(), LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);
              long startMinute =
                  getAnalysisStartMinForLogs(logsCVConfiguration, analysisStartMin, lastCVAnalysisMinute);

              if (startMinute <= 0) {
                logger.info(
                    "For account {} and CV config {} name {} type {} no data L2 clustering has happened yet. Skipping analysis",
                    logsCVConfiguration.getAccountId(), logsCVConfiguration.getUuid(), logsCVConfiguration.getName(),
                    logsCVConfiguration.getStateType());
                return;
              }

              long analysisEndMin = startMinute + CRON_POLL_INTERVAL_IN_MINUTES - 1;

              if (logsCVConfiguration.isWorkflowConfig()) {
                AnalysisContext context =
                    wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
                if (lastCVAnalysisMinute >= logsCVConfiguration.getBaselineEndMinute()) {
                  analysisEndMin = lastCVAnalysisMinute + 1;
                }

                if (analysisEndMin > logsCVConfiguration.getBaselineEndMinute() + context.getTimeDuration()) {
                  logger.info("Notifying state id: {} , corr id: {}", context.getStateExecutionId(),
                      context.getCorrelationId());
                  sendStateNotification(context, false, "", (int) analysisEndMin);
                  logger.info("Disabled 24x7 for CV Configuration with id {}", logsCVConfiguration.getUuid());
                  wingsPersistence.updateField(
                      LogsCVConfiguration.class, logsCVConfiguration.getUuid(), "enabled24x7", false);
                }
              }

              for (long l2Min = startMinute, i = 0; l2Min <= analysisEndMin; l2Min++, i++) {
                Set<String> hosts = logAnalysisService.getHostsForMinute(cvConfiguration.getAppId(),
                    LogDataRecordKeys.cvConfigId, cvConfiguration.getUuid(), l2Min, ClusterLevel.L1, ClusterLevel.H1);
                if (isNotEmpty(hosts)) {
                  logger.info(
                      "For CV config {} there is still L2 clustering pending for {} for minute {}. Skipping L2 Analysis",
                      cvConfiguration.getUuid(), hosts, l2Min);
                  return;
                }
              }

              logger.info("for {} for minute from {} to {} everything is in place, proceeding for analysis",
                  logsCVConfiguration.getUuid(), startMinute, analysisEndMin);

              String taskId = generateUuid();

              String controlInputUrl = null;
              String testInputUrl = null;
              boolean isBaselineRun = false;
              // this is the baseline prep case
              if (startMinute < logsCVConfiguration.getBaselineStartMinute()
                  || (startMinute >= logsCVConfiguration.getBaselineStartMinute()
                         && startMinute < logsCVConfiguration.getBaselineEndMinute())) {
                controlInputUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                    + LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL + "?cvConfigId="
                    + logsCVConfiguration.getUuid() + "&appId=" + logsCVConfiguration.getAppId() + "&clusterLevel="
                    + ClusterLevel.L2 + "&startMinute=" + startMinute + "&endMinute=" + analysisEndMin;
                isBaselineRun = true;
              } else {
                testInputUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                    + LogAnalysisResource.ANALYSIS_GET_24X7_ALL_LOGS_URL + "?cvConfigId="
                    + logsCVConfiguration.getUuid() + "&appId=" + logsCVConfiguration.getAppId() + "&clusterLevel="
                    + ClusterLevel.L2 + "&startMinute=" + startMinute + "&endMinute=" + analysisEndMin;
              }

              String logAnalysisSaveUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_SAVE_24X7_ANALYSIS_RECORDS_URL
                  + "?cvConfigId=" + logsCVConfiguration.getUuid() + "&appId=" + logsCVConfiguration.getAppId()
                  + "&analysisMinute=" + analysisEndMin + "&taskId=" + taskId
                  + "&comparisonStrategy=" + logsCVConfiguration.getComparisonStrategy();

              final String logAnalysisGetUrl = "/verification/" + LogAnalysisResource.LOG_ANALYSIS
                  + LogAnalysisResource.ANALYSIS_GET_24X7_ANALYSIS_RECORDS_URL
                  + "?appId=" + logsCVConfiguration.getAppId() + "&cvConfigId=" + logsCVConfiguration.getUuid()
                  + "&analysisMinute=" + ((LogsCVConfiguration) cvConfiguration).getBaselineEndMinute() + "&compressed="
                  + verificationManagerClientHelper
                        .callManagerWithRetry(verificationManagerClient.isFeatureEnabled(
                            FeatureName.SEND_LOG_ANALYSIS_COMPRESSED, accountId))
                        .getResource();
              String failureUrl = "/verification/" + LearningEngineService.RESOURCE_URL
                  + VerificationConstants.NOTIFY_LEARNING_FAILURE + "?taskId=" + taskId;

              String stateExecutionIdForLETask =
                  "LOG_24X7_ANALYSIS_" + logsCVConfiguration.getUuid() + "_" + analysisEndMin;
              learningEngineService.checkAndUpdateFailedLETask(stateExecutionIdForLETask, (int) analysisEndMin);

              if (learningEngineService.isEligibleToCreateTask(
                      stateExecutionIdForLETask, cvConfiguration.getUuid(), analysisEndMin, MLAnalysisType.LOG_ML)) {
                int nextBackoffCount = learningEngineService.getNextServiceGuardBackoffCount(
                    stateExecutionIdForLETask, cvConfiguration.getUuid(), analysisEndMin, MLAnalysisType.LOG_ML);
                LearningEngineAnalysisTask analysisTask =
                    LearningEngineAnalysisTask.builder()
                        .state_execution_id(stateExecutionIdForLETask)
                        .service_id(logsCVConfiguration.getServiceId())
                        .query(Lists.newArrayList(logsCVConfiguration.getQuery()))
                        .sim_threshold(0.9)
                        .analysis_minute(analysisEndMin)
                        .analysis_save_url(logAnalysisSaveUrl)
                        .log_analysis_get_url(logAnalysisGetUrl)
                        .analysis_failure_url(failureUrl)
                        .service_guard_backoff_count(nextBackoffCount)
                        .ml_analysis_type(MLAnalysisType.LOG_ML)
                        .test_input_url(testInputUrl)
                        .control_input_url(controlInputUrl)
                        .test_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
                        .feature_name("NEURAL_NET")
                        .is24x7Task(true)
                        .stateType(logsCVConfiguration.getStateType())
                        .cvConfigId(logsCVConfiguration.getUuid())
                        .analysis_comparison_strategy(logsCVConfiguration.getComparisonStrategy())
                        .alertThreshold(getAlertThreshold(cvConfiguration, analysisEndMin))
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

                List<MLExperiments> experiments = get24x7Experiments(MLAnalysisType.LOG_ML.name());
                for (MLExperiments experiment : experiments) {
                  LearningEngineExperimentalAnalysisTask expTask =
                      LearningEngineExperimentalAnalysisTask.builder()
                          .state_execution_id("LOG_24X7_ANALYSIS_" + logsCVConfiguration.getUuid() + "_"
                              + analysisEndMin + generateUuid())
                          .service_id(logsCVConfiguration.getServiceId())
                          .query(Lists.newArrayList(logsCVConfiguration.getQuery()))
                          .sim_threshold(0.9)
                          .analysis_minute(analysisEndMin)
                          .analysis_save_url(getSaveUrlForExperimentalTask(taskId))
                          .log_analysis_get_url(logAnalysisGetUrl)
                          .ml_analysis_type(MLAnalysisType.LOG_ML)
                          .test_input_url(isEmpty(testInputUrl) ? null : testInputUrl + "&" + IS_EXPERIMENTAL + "=true")
                          .control_input_url(
                              isEmpty(controlInputUrl) ? null : controlInputUrl + "&" + IS_EXPERIMENTAL + "=true")
                          .test_nodes(Sets.newHashSet(DUMMY_HOST_NAME))
                          .feature_name("NEURAL_NET")
                          .is24x7Task(true)
                          .stateType(logsCVConfiguration.getStateType())
                          .tolerance(cvConfiguration.getAnalysisTolerance().tolerance())
                          .cvConfigId(logsCVConfiguration.getUuid())
                          .analysis_comparison_strategy(logsCVConfiguration.getComparisonStrategy())
                          .experiment_name(experiment.getExperimentName())
                          .alertThreshold(getAlertThreshold(cvConfiguration, analysisEndMin))
                          .build();
                  expTask.setAppId(cvConfiguration.getAppId());
                  expTask.setUuid(taskId);
                  learningEngineService.addLearningEngineExperimentalAnalysisTask(expTask);
                }
              }
            } catch (Exception ex) {
              try {
                if (cvConfiguration.isWorkflowConfig()) {
                  AnalysisContext context =
                      wingsPersistence.get(AnalysisContext.class, logsCVConfiguration.getContextId());
                  logger.error("Verification L1 => L2 cluster failed", ex);
                  final VerificationStateAnalysisExecutionData executionData =
                      VerificationStateAnalysisExecutionData.builder().build();
                  executionData.setStatus(ExecutionStatus.ERROR);
                  executionData.setErrorMsg(ex.getMessage());
                  logger.info("Notifying state id: {} , corr id: {}", context.getStateExecutionId(),
                      context.getCorrelationId());
                  verificationManagerClientHelper.notifyManagerForVerificationAnalysis(
                      context, VerificationDataAnalysisResponse.builder().stateExecutionData(executionData).build());
                  wingsPersistence.updateField(CVConfiguration.class, cvConfiguration.getUuid(), "enabled24x7", false);
                }
              } catch (Exception e) {
                logger.error("Verification cluster manager cleanup failed", e);
              }
            }
          }
        });
  }

  @Override
  public void cleanupStuckLocks() {
    DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "quartz_verification_locks");
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
    if (learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
      final ExecutionStatus status = error ? ExecutionStatus.ERROR : ExecutionStatus.SUCCESS;

      VerificationStateAnalysisExecutionData logAnalysisExecutionData =
          VerificationStateAnalysisExecutionData.builder()
              .stateExecutionInstanceId(context.getStateExecutionId())
              .serverConfigId(context.getAnalysisServerConfigId())
              .query(context.getQuery())
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

      final VerificationDataAnalysisResponse response =
          VerificationDataAnalysisResponse.builder().stateExecutionData(logAnalysisExecutionData).build();
      response.setExecutionStatus(status);
      logger.info("Notifying state id: {} , corr id: {}", context.getStateExecutionId(), context.getCorrelationId());
      verificationManagerClientHelper.notifyManagerForVerificationAnalysis(context, response);
    }
  }

  @Override
  public void triggerTimeSeriesAlertIfNecessary(String cvConfigId, double riskScore, long analysisMinute) {
    if (isEmpty(cvConfigId)) {
      return;
    }
    final CVConfiguration cvConfiguration = wingsPersistence.get(CVConfiguration.class, cvConfigId);
    Preconditions.checkNotNull(cvConfiguration, "no config found with id " + cvConfigId);

    if (!shouldThrowAlert(cvConfiguration)) {
      return;
    }
    if (riskScore <= cvConfiguration.getAlertThreshold()) {
      logger.info("for {} the risk {} is lower than the threshold {}. All open alerts will be closed.", cvConfigId,
          riskScore, cvConfiguration.getAlertThreshold());

      verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.closeCVAlert(cvConfigId,
          ContinuousVerificationAlertData.builder()
              .riskScore(riskScore)
              .mlAnalysisType(MLAnalysisType.TIME_SERIES)
              .alertStatus(AlertStatus.Closed)
              .analysisStartTime(TimeUnit.MINUTES.toMillis(analysisMinute - CRON_POLL_INTERVAL_IN_MINUTES) + 1)
              .analysisEndTime(TimeUnit.MINUTES.toMillis(analysisMinute))
              .build()));
      return;
    }

    logger.info("triggering alert for {} with risk score {}", cvConfigId, riskScore);
    verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerCVAlert(cvConfigId,
        ContinuousVerificationAlertData.builder()
            .riskScore(riskScore)
            .mlAnalysisType(MLAnalysisType.TIME_SERIES)
            .alertStatus(AlertStatus.Open)
            .analysisStartTime(TimeUnit.MINUTES.toMillis(analysisMinute - CRON_POLL_INTERVAL_IN_MINUTES) + 1)
            .analysisEndTime(TimeUnit.MINUTES.toMillis(analysisMinute))
            .build()));
  }

  @Override
  public void triggerLogAnalysisAlertIfNecessary(
      String cvConfigId, LogMLAnalysisRecord mlAnalysisResponse, int analysisMinute) {
    if (isEmpty(cvConfigId)) {
      return;
    }
    if (isEmpty(mlAnalysisResponse.getUnknown_clusters())) {
      logger.info("No unknown clusters for {} for min {}. No alerts will be triggered");
      return;
    }

    final CVConfiguration cvConfiguration = wingsPersistence.get(CVConfiguration.class, cvConfigId);
    Preconditions.checkNotNull(cvConfiguration, "no config found with id " + cvConfigId);

    if (!shouldThrowAlert(cvConfiguration)) {
      return;
    }

    logger.info("triggering alerts for {} with unknown clusters {}", cvConfigId,
        mlAnalysisResponse.getUnknown_clusters().size());

    LogsCVConfiguration logsCVConfiguration = (LogsCVConfiguration) cvConfiguration;

    if (logsCVConfiguration.is247LogsV2()) {
      Map<Integer, LogAnalysisResult> logAnalysisResult = mlAnalysisResponse.getLog_analysis_result();

      mlAnalysisResponse.getUnknown_clusters().forEach((clusterLabel, analysisClusterMap) -> {
        if (isNotEmpty(analysisClusterMap)) {
          final SplunkAnalysisCluster splunkAnalysisCluster =
              analysisClusterMap.entrySet().iterator().next().getValue();
          verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerCVAlert(cvConfigId,
              ContinuousVerificationAlertData.builder()
                  .mlAnalysisType(MLAnalysisType.LOG_ML)
                  .logAnomaly(splunkAnalysisCluster.getText())
                  .tag(logAnalysisResult.get(splunkAnalysisCluster.getCluster_label()).getTag())
                  .analysisStartTime(TimeUnit.MINUTES.toMillis(analysisMinute - CRON_POLL_INTERVAL_IN_MINUTES) + 1)
                  .analysisEndTime(TimeUnit.MINUTES.toMillis(analysisMinute))
                  .riskScore(1.0)
                  .build()));
        }
      });
    } else {
      mlAnalysisResponse.getUnknown_clusters().forEach((clusterLabel, analysisClusterMap) -> {
        if (isNotEmpty(analysisClusterMap)) {
          final SplunkAnalysisCluster splunkAnalysisCluster =
              analysisClusterMap.entrySet().iterator().next().getValue();
          verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerCVAlert(cvConfigId,
              ContinuousVerificationAlertData.builder()
                  .mlAnalysisType(MLAnalysisType.LOG_ML)
                  .logAnomaly(splunkAnalysisCluster.getText())
                  .hosts(analysisClusterMap.keySet())
                  .analysisStartTime(TimeUnit.MINUTES.toMillis(analysisMinute - CRON_POLL_INTERVAL_IN_MINUTES) + 1)
                  .analysisEndTime(TimeUnit.MINUTES.toMillis(analysisMinute))
                  .riskScore(1.0)
                  .build()));
        }
      });
    }
  }

  @Override
  public void processNextCVTasks(String accountId) {
    while (true) {
      Optional<CVTask> cvTask = cvTaskService.getNextTask(accountId);
      if (cvTask.isPresent()) {
        verificationManagerClientHelper.callManagerWithRetry(
            verificationManagerClient.collectCVData(cvTask.get().getUuid(), cvTask.get().getDataCollectionInfo()));
      } else {
        break;
      }
    }
  }

  @Override
  public void expireLongRunningCVTasks(String accountId) {
    cvTaskService.expireLongRunningTasks(accountId);
  }

  @Override
  public void retryCVTasks(String accountId) {
    cvTaskService.retryTasks(accountId);
  }

  private boolean shouldThrowAlert(CVConfiguration cvConfiguration) {
    if (!cvConfiguration.isAlertEnabled()) {
      logger.info("for {} the alert is not enabled. Returning", cvConfiguration.getUuid());
      return false;
    }

    final long currentTime = System.currentTimeMillis();
    if (cvConfiguration.getSnoozeStartTime() > 0 && cvConfiguration.getSnoozeEndTime() > 0
        && currentTime >= cvConfiguration.getSnoozeStartTime() && currentTime <= cvConfiguration.getSnoozeEndTime()) {
      logger.info("for {} the current time is in the range of snooze time {} to {}. No alerts will be triggered.",
          cvConfiguration.getUuid(), cvConfiguration.getSnoozeStartTime(), cvConfiguration.getSnoozeEndTime());
      return false;
    }

    return true;
  }
}
