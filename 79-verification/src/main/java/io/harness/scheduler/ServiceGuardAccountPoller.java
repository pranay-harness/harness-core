package io.harness.scheduler;

import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.jobs.housekeeping.UsageMetricsJob.VERIFICATION_METRIC_CRON_GROUP;
import static io.harness.jobs.housekeeping.UsageMetricsJob.VERIFICATION_METRIC_CRON_NAME;
import static io.harness.jobs.sg247.collection.ServiceGuardDataCollectionJob.SERVICE_GUARD_DATA_COLLECTION_CRON;
import static io.harness.jobs.sg247.logs.ServiceGuardLogAnalysisJob.SERVICE_GUARD_LOG_ANALYSIS_CRON;
import static io.harness.jobs.sg247.timeseries.ServiceGuardTimeSeriesAnalysisJob.SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON;
import static io.harness.jobs.workflow.collection.CVDataCollectionJob.CV_TASK_CRON;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_EXP_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_WORKFLOW_TASK_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_WORKFLOW_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.service.impl.analysis.MLAnalysisType.LOG_ML;
import static software.wings.service.impl.analysis.MLAnalysisType.TIME_SERIES;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.persistence.HIterator;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskKeys;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.intfc.verification.CVConfigurationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class ServiceGuardAccountPoller {
  private static final int POLL_INTIAL_DELAY_SEOONDS = 60;
  @Inject @Named("verificationServiceExecutor") protected ScheduledExecutorService executorService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private ContinuousVerificationService continuousVerificationService;
  private List<Account> lastAvailableAccounts = new ArrayList<>();
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HarnessMetricRegistry metricRegistry;

  public void scheduleUsageMetricsCollection() {
    executorService.scheduleAtFixedRate(
        () -> recordQueuedTaskMetric(), POLL_INTIAL_DELAY_SEOONDS, POLL_INTIAL_DELAY_SEOONDS, TimeUnit.SECONDS);
  }

  @VisibleForTesting
  void recordQueuedTaskMetric() {
    String env = System.getenv("ENV");
    if (isNotEmpty(env)) {
      env = env.replaceAll("-", "_");
    }
    LearningEngineAnalysisTask lastQueuedAnalysisTask =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
            .order(Sort.ascending("createdAt"))
            .get();

    long taskQueuedTimeInSeconds = lastQueuedAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastQueuedAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine task has been queued for {} Seconds", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS), null, taskQueuedTimeInSeconds);

    lastQueuedAnalysisTask = wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
                                 .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
                                 .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, MLAnalysisType.LOG_CLUSTER)
                                 .order(Sort.ascending("createdAt"))
                                 .get();

    taskQueuedTimeInSeconds = lastQueuedAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastQueuedAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine clustering task has been queued for {} Seconds", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS), null, taskQueuedTimeInSeconds);

    long clusterTaskCount = wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
                                .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
                                .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, MLAnalysisType.LOG_CLUSTER)
                                .count();
    logger.info("Cluster task queued count is {}", clusterTaskCount);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_COUNT), null, clusterTaskCount);

    lastQueuedAnalysisTask = wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
                                 .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
                                 .field(LearningEngineAnalysisTaskKeys.ml_analysis_type)
                                 .in(Lists.newArrayList(LOG_ML, MLAnalysisType.TIME_SERIES))
                                 .order(Sort.ascending("createdAt"))
                                 .get();

    taskQueuedTimeInSeconds = lastQueuedAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastQueuedAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine analysis task has been queued for {} Seconds", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS), null, taskQueuedTimeInSeconds);

    long analysisTaskCount = wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
                                 .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
                                 .field(LearningEngineAnalysisTaskKeys.ml_analysis_type)
                                 .in(Lists.newArrayList(LOG_ML, MLAnalysisType.TIME_SERIES))
                                 .count();
    logger.info("Analysis task queued count is {}", analysisTaskCount);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_COUNT), null, analysisTaskCount);

    lastQueuedAnalysisTask =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, MLAnalysisType.FEEDBACK_ANALYSIS)
            .order(Sort.ascending("createdAt"))
            .get();

    taskQueuedTimeInSeconds = lastQueuedAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastQueuedAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine feedback task has been queued for {} Seconds", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_TIME_IN_SECONDS), null, taskQueuedTimeInSeconds);

    long feedbackTaskCount =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, MLAnalysisType.FEEDBACK_ANALYSIS)
            .count();
    logger.info("Feedback task queued count is {}", analysisTaskCount);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_COUNT), null, feedbackTaskCount);

    // Do the same for experimental
    LearningEngineExperimentalAnalysisTask lastQueuedExpAnalysisTask =
        wingsPersistence.createQuery(LearningEngineExperimentalAnalysisTask.class, excludeAuthority)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
            .order(Sort.ascending("createdAt"))
            .get();

    long taskQueuedTimeInSecondsExp = lastQueuedExpAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastQueuedExpAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine Experimental task has been queued for {} Seconds", taskQueuedTimeInSecondsExp);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_EXP_TASK_QUEUED_TIME_IN_SECONDS), null, taskQueuedTimeInSecondsExp);
    recordServiceGuardMetrics(env);
    recordWorkflowVerificationMetrics(env);
  }

  private String getMetricName(String env, String metricName) {
    if (isEmpty(env)) {
      return metricName;
    }

    return env + "_" + metricName;
  }

  private Query<LearningEngineAnalysisTask> createLETaskQuery(
      boolean is24x7Task, List<MLAnalysisType> analysisTypeList) {
    return wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
        .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
        .field(LearningEngineAnalysisTaskKeys.ml_analysis_type)
        .in(analysisTypeList)
        .filter(LearningEngineAnalysisTaskKeys.is24x7Task, is24x7Task);
  }

  private void recordServiceGuardMetrics(String env) {
    // Metric for last queued time of Analysis Service guard tasks
    LearningEngineAnalysisTask lastQueuedServiceGuardAnalysisTask =
        createLETaskQuery(true, Lists.newArrayList(LOG_ML, MLAnalysisType.TIME_SERIES))
            .order(Sort.ascending("createdAt"))
            .get();

    long taskQueuedTimeInSeconds = lastQueuedServiceGuardAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(
              System.currentTimeMillis() - lastQueuedServiceGuardAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine Service Guard analysis task has been queued for {} Seconds", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS), null,
        taskQueuedTimeInSeconds);

    // Metric for queue count of Analysis Service guard tasks
    long serviceGuardAnalysisTaskCount =
        createLETaskQuery(true, Lists.newArrayList(LOG_ML, MLAnalysisType.TIME_SERIES)).count();
    logger.info("Service guard Analysis task queued count is {}", serviceGuardAnalysisTaskCount);
    metricRegistry.recordGaugeValue(getMetricName(env, LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_COUNT), null,
        serviceGuardAnalysisTaskCount);

    // Metric for queue count of Clustering Service guard tasks
    long clusterTaskCount = createLETaskQuery(true, Lists.newArrayList(MLAnalysisType.LOG_CLUSTER)).count();
    logger.info("Cluster task queued count is {}", clusterTaskCount);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_COUNT), null, clusterTaskCount);

    // Metric for last queued time of Clustering Service guard tasks
    LearningEngineAnalysisTask lastQueuedAnalysisTask =
        createLETaskQuery(true, Lists.newArrayList(MLAnalysisType.LOG_CLUSTER))
            .order(Sort.ascending("createdAt"))
            .get();

    taskQueuedTimeInSeconds = lastQueuedAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastQueuedAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine clustering task has been queued for {} Seconds", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS), null,
        taskQueuedTimeInSeconds);
  }

  private void recordWorkflowVerificationMetrics(String env) {
    long workflowTaskCount =
        createLETaskQuery(false, Lists.newArrayList(MLAnalysisType.LOG_CLUSTER, LOG_ML, TIME_SERIES)).count();
    LearningEngineAnalysisTask oldestWorkflowTask =
        createLETaskQuery(false, Lists.newArrayList(MLAnalysisType.LOG_CLUSTER, LOG_ML, TIME_SERIES))
            .order("createdAt")
            .get();
    long taskQueuedTimeInSeconds = oldestWorkflowTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - oldestWorkflowTask.getCreatedAt())
        : 0;
    logger.info("Number of workflow LE tasks currently queued is {}", workflowTaskCount);
    logger.info("Waiting time for workflow LE task currently is {}", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(
        getMetricName(env, LEARNING_ENGINE_WORKFLOW_TASK_QUEUED_TIME_IN_SECONDS), null, taskQueuedTimeInSeconds);

    metricRegistry.recordGaugeValue(getMetricName(env, LEARNING_ENGINE_WORKFLOW_TASK_COUNT), null, workflowTaskCount);
  }

  public void deleteServiceGuardCrons() {
    logger.info("Deleting crons for all accounts");
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();
        if (jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_DATA_COLLECTION_CRON)) {
          jobScheduler.deleteJob(account.getUuid(), SERVICE_GUARD_DATA_COLLECTION_CRON);
          logger.info("Deleting crons for account {} ", account.getUuid());
        }

        if (jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON)) {
          jobScheduler.deleteJob(account.getUuid(), SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON);
          logger.info("Deleting crons for account {} ", account.getUuid());
        }

        if (jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_LOG_ANALYSIS_CRON)) {
          jobScheduler.deleteJob(account.getUuid(), SERVICE_GUARD_LOG_ANALYSIS_CRON);
          logger.info("Deleting crons for account {} ", account.getUuid());
        }
        if (jobScheduler.checkExists(account.getUuid(), CV_TASK_CRON)) {
          jobScheduler.deleteJob(account.getUuid(), CV_TASK_CRON);
          logger.info("Deleting crons for account {} ", account.getUuid());
        }
      }
    }

    logger.info("Delete usage metric job");
    if (jobScheduler.checkExists(VERIFICATION_METRIC_CRON_NAME, VERIFICATION_METRIC_CRON_GROUP)) {
      jobScheduler.deleteJob(VERIFICATION_METRIC_CRON_NAME, VERIFICATION_METRIC_CRON_GROUP);
    }
  }

  private void cleanUpAfterDeletionOfEntity() {
    cvConfigurationService.deleteStaleConfigs();
  }
}
