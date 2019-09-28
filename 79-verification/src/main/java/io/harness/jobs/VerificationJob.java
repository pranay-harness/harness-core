package io.harness.jobs;

import static io.harness.jobs.LogDataProcessorJob.LOG_DATA_PROCESSOR_CRON_GROUP;
import static io.harness.jobs.MetricDataAnalysisJob.METRIC_DATA_ANALYSIS_CRON_GROUP;
import static io.harness.jobs.MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP;
import static io.harness.jobs.sg247.collection.ServiceGuardDataCollectionJob.SERVICE_GUARD_DATA_COLLECTION_CRON;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import software.wings.beans.Account;

import java.util.List;

/**
 * Verification job that handles scheduling jobs related to APM and Logs
 *
 * Created by Pranjal on 10/04/2018
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Slf4j
@Deprecated
public class VerificationJob implements Job {
  // Cron name to uniquely identify the cron
  public static final String VERIFICATION_CRON_NAME = "VERIFICATION_CRON_NAME";
  // Cron Group name
  public static final String VERIFICATION_CRON_GROUP = "VERIFICATION_CRON_GROUP";

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public void execute(JobExecutionContext JobExecutionContext) {
    logger.warn("Deprecating Verification Job .. New Job is ServiceGuardMainJob");
  }

  public static void removeJob(PersistentScheduler jobScheduler) {
    jobScheduler.deleteJob(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP);
  }

  public static void deleteCrons(List<Account> disabledAccounts, PersistentScheduler jobScheduler) {
    logger.info("Deleting crons for " + disabledAccounts.size() + " accounts");
    disabledAccounts.forEach(account -> {
      if (jobScheduler.checkExists(account.getUuid(), SERVICE_GUARD_DATA_COLLECTION_CRON)) {
        jobScheduler.deleteJob(account.getUuid(), SERVICE_GUARD_DATA_COLLECTION_CRON);
        logger.info("Deleting old crons for account {} ", account.getUuid());
      }

      if (jobScheduler.checkExists(account.getUuid(), METRIC_DATA_PROCESSOR_CRON_GROUP)) {
        jobScheduler.deleteJob(account.getUuid(), METRIC_DATA_PROCESSOR_CRON_GROUP);
        logger.info("Deleting old crons for account {} ", account.getUuid());
      }

      if (jobScheduler.checkExists(account.getUuid(), LOG_DATA_PROCESSOR_CRON_GROUP)) {
        jobScheduler.deleteJob(account.getUuid(), LOG_DATA_PROCESSOR_CRON_GROUP);
        logger.info("Deleting old crons for account {} ", account.getUuid());
      }

      if (jobScheduler.checkExists(account.getUuid(), METRIC_DATA_ANALYSIS_CRON_GROUP)) {
        jobScheduler.deleteJob(account.getUuid(), METRIC_DATA_ANALYSIS_CRON_GROUP);
        logger.info("Deleting old crons for account {} ", account.getUuid());
      }
    });
  }
}
