package io.harness.jobs.housekeeping;

import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.common.VerificationConstants.DEFAULT_LE_AUTOSCALE_DATA_COLLECTION_INTERVAL_IN_SECONDS;
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

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.metrics.HarnessMetricRegistry;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask.LearningEngineAnalysisTaskKeys;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Job that runs every 1 minute and records verification related metrics
 *
 * Created by Pranjal on 02/26/2019
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Slf4j
public class UsageMetricsJob implements Job {
  // Cron name to uniquely identify the cron
  public static final String VERIFICATION_METRIC_CRON_NAME = "USAGE_METRIC_CRON";
  // Cron Group name
  public static final String VERIFICATION_METRIC_CRON_GROUP = "USAGE_METRIC_CRON";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private HarnessMetricRegistry metricRegistry;

  @Override
  public void execute(JobExecutionContext JobExecutionContext) {
    recordQueuedTaskMetric();
  }

  private void recordQueuedTaskMetric() {
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
    metricRegistry.recordGaugeValue(LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(
          env + "_" + LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    }

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
        LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(
          env + "_" + LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    }

    long clusterTaskCount = wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
                                .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
                                .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, MLAnalysisType.LOG_CLUSTER)
                                .count();
    logger.info("Cluster task queued count is {}", clusterTaskCount);
    metricRegistry.recordGaugeValue(LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_COUNT, null, clusterTaskCount);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(env + "_" + LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_COUNT, null, clusterTaskCount);
    }

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
        LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(
          env + "_" + LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    }

    long analysisTaskCount = wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
                                 .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
                                 .field(LearningEngineAnalysisTaskKeys.ml_analysis_type)
                                 .in(Lists.newArrayList(LOG_ML, MLAnalysisType.TIME_SERIES))
                                 .count();
    logger.info("Analysis task queued count is {}", analysisTaskCount);
    metricRegistry.recordGaugeValue(LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_COUNT, null, analysisTaskCount);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(env + "_" + LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_COUNT, null, analysisTaskCount);
    }

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
        LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(
          env + "_" + LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    }

    long feedbackTaskCount =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
            .filter(LearningEngineAnalysisTaskKeys.ml_analysis_type, MLAnalysisType.FEEDBACK_ANALYSIS)
            .count();
    logger.info("Feedback task queued count is {}", analysisTaskCount);
    metricRegistry.recordGaugeValue(LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_COUNT, null, feedbackTaskCount);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(env + "_" + LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_COUNT, null, feedbackTaskCount);
    }

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
    metricRegistry.recordGaugeValue(LEARNING_ENGINE_EXP_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSecondsExp);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(
          env + "_" + LEARNING_ENGINE_EXP_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    }
    recordServiceGuardMetrics(env);
    recordWorkflowVerificationMetrics(env);
  }

  private Query<LearningEngineAnalysisTask> createLETaskQuery(
      Boolean is24x7Task, List<MLAnalysisType> analysisTypeList) {
    Query<LearningEngineAnalysisTask> query =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
            .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
            .field(LearningEngineAnalysisTaskKeys.ml_analysis_type)
            .in(analysisTypeList);
    if (is24x7Task == null) {
      query = query.field(LearningEngineAnalysisTaskKeys.is24x7Task).doesNotExist();
    } else {
      query = query.filter(LearningEngineAnalysisTaskKeys.is24x7Task, is24x7Task);
    }
    return query;
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
        LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(env + "_" + LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS,
          null, taskQueuedTimeInSeconds);
    }

    // Metric for queue count of Analysis Service guard tasks
    long serviceGuardAnalysisTaskCount =
        createLETaskQuery(true, Lists.newArrayList(LOG_ML, MLAnalysisType.TIME_SERIES)).count();
    logger.info("Service guard Analysis task queued count is {}", serviceGuardAnalysisTaskCount);
    metricRegistry.recordGaugeValue(
        LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_COUNT, null, serviceGuardAnalysisTaskCount);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(
          env + "_" + LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_COUNT, null, serviceGuardAnalysisTaskCount);
    }

    // Metric for queue count of Clustering Service guard tasks
    long clusterTaskCount = createLETaskQuery(true, Lists.newArrayList(MLAnalysisType.LOG_CLUSTER)).count();
    logger.info("Cluster task queued count is {}", clusterTaskCount);
    metricRegistry.recordGaugeValue(LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_COUNT, null, clusterTaskCount);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(
          env + "_" + LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_COUNT, null, clusterTaskCount);
    }

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
        LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(env + "_" + LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS,
          null, taskQueuedTimeInSeconds);
    }
  }

  private void recordWorkflowVerificationMetrics(String env) {
    long workflowTaskCount =
        createLETaskQuery(null, Lists.newArrayList(MLAnalysisType.LOG_CLUSTER, LOG_ML, TIME_SERIES)).count();
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
        LEARNING_ENGINE_WORKFLOW_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(
          env + "_" + LEARNING_ENGINE_WORKFLOW_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
    }

    metricRegistry.recordGaugeValue(LEARNING_ENGINE_WORKFLOW_TASK_COUNT, null, workflowTaskCount);
    if (isNotEmpty(env)) {
      metricRegistry.recordGaugeValue(env + "_" + LEARNING_ENGINE_WORKFLOW_TASK_COUNT, null, workflowTaskCount);
    }
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    if (!jobScheduler.checkExists(VERIFICATION_METRIC_CRON_NAME, VERIFICATION_METRIC_CRON_GROUP)) {
      JobDetail job = JobBuilder.newJob(UsageMetricsJob.class)
                          .withIdentity(VERIFICATION_METRIC_CRON_NAME, VERIFICATION_METRIC_CRON_GROUP)
                          .withDescription("Verification job ")
                          .build();
      Trigger trigger =
          TriggerBuilder.newTrigger()
              .withIdentity(VERIFICATION_METRIC_CRON_NAME, VERIFICATION_METRIC_CRON_GROUP)
              .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(DEFAULT_LE_AUTOSCALE_DATA_COLLECTION_INTERVAL_IN_SECONDS)
                                .repeatForever())
              .build();
      jobScheduler.scheduleJob(job, trigger);
      logger.info("Added UsageMetricsJob with details : {}", job);
    }
  }
}
