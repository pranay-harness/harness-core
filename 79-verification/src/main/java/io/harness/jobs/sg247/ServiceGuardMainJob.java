package io.harness.jobs.sg247;

import static io.harness.jobs.sg247.collection.ServiceGuardDataCollectionJob.SERVICE_GUARD_DATA_COLLECTION_CRON;
import static io.harness.jobs.sg247.logs.ServiceGuardLogAnalysisJob.SERVICE_GUARD_LOG_ANALYSIS_CRON;
import static io.harness.jobs.sg247.timeseries.ServiceGuardTimeSeriesAnalysisJob.SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.jobs.VerificationJob;
import io.harness.jobs.sg247.collection.ServiceGuardDataCollectionJob;
import io.harness.jobs.sg247.logs.ServiceGuardLogAnalysisJob;
import io.harness.jobs.sg247.timeseries.ServiceGuardTimeSeriesAnalysisJob;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.scheduler.PersistentScheduler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.service.intfc.verification.CVConfigurationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Verification job that handles scheduling jobs related to APM and Logs
 *
 * Created by Pranjal on 10/04/2018
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Slf4j
public class ServiceGuardMainJob implements Job {
  // Cron name to uniquely identify the cron
  public static final String SERVICE_GUARD_MAIN_CRON = "SERVICE_GUARD_MAIN_CRON";

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject private VerificationManagerClient verificationManagerClient;

  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;

  @Inject private CVConfigurationService cvConfigurationService;

  @Inject private ContinuousVerificationService continuousVerificationService;

  private List<Account> lastAvailableAccounts = new ArrayList<>();

  @Override
  public void execute(JobExecutionContext JobExecutionContext) {
    logger.info("Running Verification Job to schedule APM and Log processing jobs");
    continuousVerificationService.cleanupStuckLocks();
    // need to fetch accounts
    // Single api call to fetch both Enabled and disabled accounts
    // As this being a paginated request, it fetches max of 50 accounts at a time.
    PageResponse<Account> accounts;
    int offset = 0;
    List<Account> accountsFetched = new ArrayList<>();
    int numOfAccountsFetched;
    do {
      accounts = verificationManagerClientHelper
                     .callManagerWithRetry(verificationManagerClient.getAccounts(String.valueOf(offset)))
                     .getResource();
      logger.info("Trying to Delete crons for Disabled Accounts");
      VerificationJob.deleteCrons(accounts, jobScheduler);
      accountsFetched.addAll(accounts);
      numOfAccountsFetched = accounts.size();
      // get all the disabled account and delete the APM and log cron's
      List<Account> enabledAccounts =
          accounts.stream()
              .filter(account
                  -> account.getLicenseInfo() == null
                      || ((account.getLicenseInfo() != null)
                             && (account.getLicenseInfo().getAccountStatus().equals(AccountStatus.ACTIVE)
                                    && (account.getLicenseInfo().getAccountType().equals(AccountType.TRIAL)
                                           || account.getLicenseInfo().getAccountType().equals(AccountType.PAID)))))
              .collect(Collectors.toList());

      // remove all the enabled accounts from the accounts List. And disable cron's for all disabled accounts
      accounts.removeAll(enabledAccounts);
      logger.info("Trying to Delete crons for Disabled Accounts");
      deleteCrons(accounts);

      // schedule APM and log cron's
      triggerServiceGuardCrons(enabledAccounts);
      logger.info("Completed scheduling APM and Log processing jobs");
      offset = offset + PageRequest.DEFAULT_PAGE_SIZE;
    } while (numOfAccountsFetched >= PageRequest.DEFAULT_PAGE_SIZE);
    lastAvailableAccounts.removeAll(accountsFetched);
    // delete lastAvailableAccounts that are no longer available
    logger.info("Trying to Delete crons for Deleted Accounts");
    deleteCrons(lastAvailableAccounts);
    logger.info("Deleting any stale configurations if available.");
    cleanUpAfterDeletionOfEntity();
    lastAvailableAccounts = accountsFetched;
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    if (!jobScheduler.checkExists(SERVICE_GUARD_MAIN_CRON, SERVICE_GUARD_MAIN_CRON)) {
      JobDetail job = JobBuilder.newJob(ServiceGuardMainJob.class)
                          .withIdentity(SERVICE_GUARD_MAIN_CRON, SERVICE_GUARD_MAIN_CRON)
                          .withDescription("Verification job ")
                          .build();
      Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(SERVICE_GUARD_MAIN_CRON, SERVICE_GUARD_MAIN_CRON)
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                              .withIntervalInSeconds((int) (CRON_POLL_INTERVAL / 2))
                                              .repeatForever())
                            .build();
      jobScheduler.scheduleJob(job, trigger);
      logger.info("Added job with details : {}", job);
    }
  }

  public void triggerServiceGuardCrons(List<Account> enabledAccounts) {
    logger.info("Triggering service Guard crons for " + enabledAccounts.size() + " enabled accounts");
    enabledAccounts.forEach(account -> {
      scheduleServiceGuardDataCollectionsCronsJobs(account.getUuid());
      scheduleServiceGuardTimeSeriesCronJobs(account.getUuid());
      scheduleServiceGuardLogCronJobs(account.getUuid());
    });
  }

  private void scheduleServiceGuardDataCollectionsCronsJobs(String accountId) {
    if (!jobScheduler.checkExists(accountId, SERVICE_GUARD_DATA_COLLECTION_CRON)) {
      Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
      JobDetail job = JobBuilder.newJob(ServiceGuardDataCollectionJob.class)
                          .withIdentity(accountId, SERVICE_GUARD_DATA_COLLECTION_CRON)
                          .usingJobData("timestamp", System.currentTimeMillis())
                          .usingJobData("accountId", accountId)
                          .build();

      Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(accountId, SERVICE_GUARD_DATA_COLLECTION_CRON)
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                              .withIntervalInSeconds(30)
                                              .withMisfireHandlingInstructionNowWithExistingCount()
                                              .repeatForever())
                            .startAt(startDate)
                            .build();

      jobScheduler.scheduleJob(job, trigger);
      logger.info("Scheduled APM data collection Cron Job for Account : {}, with details : {}", accountId, job);
    }
  }

  private void scheduleServiceGuardTimeSeriesCronJobs(String accountId) {
    if (!jobScheduler.checkExists(accountId, SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON)) {
      Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
      JobDetail job = JobBuilder.newJob(ServiceGuardTimeSeriesAnalysisJob.class)
                          .withIdentity(accountId, SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON)
                          .usingJobData("timestamp", System.currentTimeMillis())
                          .usingJobData("accountId", accountId)
                          .build();

      Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(accountId, SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON)
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                              .withIntervalInSeconds(30)
                                              .withMisfireHandlingInstructionNowWithExistingCount()
                                              .repeatForever())
                            .startAt(startDate)
                            .build();

      jobScheduler.scheduleJob(job, trigger);
      logger.info("Scheduled APM data collection Cron Job for Account : {}, with details : {}", accountId, job);
    }
  }

  private void scheduleServiceGuardLogCronJobs(String accountId) {
    if (jobScheduler.checkExists(accountId, SERVICE_GUARD_LOG_ANALYSIS_CRON)) {
      return;
    }
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
    JobDetail job = JobBuilder.newJob(ServiceGuardLogAnalysisJob.class)
                        .withIdentity(accountId, SERVICE_GUARD_LOG_ANALYSIS_CRON)
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("accountId", accountId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(accountId, SERVICE_GUARD_LOG_ANALYSIS_CRON)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds((int) (CRON_POLL_INTERVAL / 10))
                                            .withMisfireHandlingInstructionNowWithExistingCount()
                                            .repeatForever())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled Log data collection Cron Job for Account : {}, with details : {}", accountId, job);
  }

  public void deleteCrons(List<Account> disabledAccounts) {
    logger.info("Deleting crons for " + disabledAccounts.size() + " accounts");
    disabledAccounts.forEach(account -> {
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
    });
  }

  private void cleanUpAfterDeletionOfEntity() {
    cvConfigurationService.deleteStaleConfigs();
  }

  @VisibleForTesting
  public void setQuartzScheduler(PersistentScheduler jobScheduler) {
    this.jobScheduler = jobScheduler;
  }
}
