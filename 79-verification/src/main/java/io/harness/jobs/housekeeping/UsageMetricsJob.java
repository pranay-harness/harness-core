package io.harness.jobs.housekeeping;

import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.common.VerificationConstants.DEFAULT_LE_AUTOSCALE_DATA_COLLECTION_INTERVAL_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_EXP_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.metrics.HarnessMetricRegistry;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
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

    lastQueuedAnalysisTask = wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
                                 .filter(LearningEngineAnalysisTaskKeys.executionStatus, QUEUED)
                                 .field(LearningEngineAnalysisTaskKeys.ml_analysis_type)
                                 .in(Lists.newArrayList(MLAnalysisType.LOG_ML, MLAnalysisType.TIME_SERIES))
                                 .order(Sort.ascending("createdAt"))
                                 .get();

    taskQueuedTimeInSeconds = lastQueuedAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastQueuedAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine analysis task has been queued for {} Seconds", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(
        LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);

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
