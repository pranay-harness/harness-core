package io.harness.jobs;

import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.common.VerificationConstants.DEFAULT_LE_AUTOSCALE_DATA_COLLECTION_INTERVAL_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS;

import com.google.inject.Inject;

import io.harness.metrics.HarnessMetricRegistry;
import io.harness.scheduler.PersistentScheduler;
import io.harness.time.Timestamp;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;

import java.util.concurrent.TimeUnit;

/**
 * Job that runs every 1 minute and records verification related metrics
 *
 * Created by Pranjal on 02/26/2019
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class VerificationMetricJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(VerificationMetricJob.class);

  // Cron name to uniquely identify the cron
  public static final String VERIFICATION_METRIC_CRON_NAME = "VERIFICATION_METRIC_CRON_NAME";
  // Cron Group name
  public static final String VERIFICATION_METRIC_CRON_GROUP = "VERIFICATION_METRIC_CRON_GROUP";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private HarnessMetricRegistry metricRegistry;

  @Override
  public void execute(JobExecutionContext JobExecutionContext) {
    recordQueuedTaskMetric();
  }

  private void recordQueuedTaskMetric() {
    LearningEngineAnalysisTask lastQueuedAnalysisTask =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
            .filter("executionStatus", QUEUED)
            .order(Sort.ascending("createdAt"))
            .get();

    long taskQueuedTimeInSeconds = lastQueuedAnalysisTask != null
        ? TimeUnit.MILLISECONDS.toSeconds(Timestamp.currentMinuteBoundary() - lastQueuedAnalysisTask.getCreatedAt())
        : 0;
    logger.info("Learning Engine task has been queued for {} Seconds", taskQueuedTimeInSeconds);
    metricRegistry.recordGaugeValue(LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS, null, taskQueuedTimeInSeconds);
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    if (!jobScheduler.checkExists(VERIFICATION_METRIC_CRON_NAME, VERIFICATION_METRIC_CRON_GROUP)) {
      JobDetail job = JobBuilder.newJob(VerificationMetricJob.class)
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
      logger.info("Added VerificationMetricJob with details : {}", job);
    }
  }
}
