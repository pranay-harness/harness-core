package io.harness.jobs;

import static io.harness.jobs.LogDataProcessorJob.LOG_DATA_PROCESSOR_CRON_GROUP;
import static io.harness.jobs.MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.scheduler.PersistentScheduler;
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
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureName;

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
public class VerificationJob implements Job {
  // Cron name to uniquely identify the cron
  public static final String VERIFICATION_CRON_NAME = "VERIFICATION_CRON_NAME";
  // Cron Group name
  public static final String VERIFICATION_CRON_GROUP = "VERIFICATION_CRON_GROUP";

  @Inject @Named("JobScheduler") private PersistentScheduler jobScheduler;
  private static final Logger logger = LoggerFactory.getLogger(VerificationJob.class);

  @Inject private VerificationManagerClient verificationManagerClient;

  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;

  private List<Account> lastAvailableAccounts = new ArrayList<>();

  @Override
  public void execute(JobExecutionContext JobExecutionContext) {
    logger.info("Running Verification Job to schedule APM and Log processing jobs");
    // need to fetch accounts
    // Single api call to fetch both Enabled and disabled accounts
    // As this being a paginated request, it fetches max of 50 accounts at a time.
    PageResponse<Account> accounts;
    int offSet = 0;
    List<Account> accountsFetched = new ArrayList<>();
    do {
      accounts = verificationManagerClientHelper
                     .callManagerWithRetry(verificationManagerClient.getAccounts(String.valueOf(offSet)))
                     .getResource();
      accountsFetched.addAll(accounts);
      // get all the disabled account and delete the APM and log cron's
      List<Account> enabledAccounts =
          accounts.stream()
              .filter(account
                  -> (account.getLicenseInfo() != null)
                      && (account.getLicenseInfo().getAccountStatus().equals(AccountStatus.ACTIVE)
                             && (account.getLicenseInfo().getAccountType().equals(AccountType.TRIAL)
                                    || account.getLicenseInfo().getAccountType().equals(AccountType.PAID))))
              .collect(Collectors.toList());

      // remove all the enabled accounts from the accounts List. And disable cron's for all disabled accounts
      accounts.removeAll(enabledAccounts);
      logger.info("Trying to Delete crons for Disabled Accounts");
      deleteCrons(accounts);

      // schedule APM and log cron's
      triggerDataProcessorCron(enabledAccounts);
      logger.info("Completed scheduling APM and Log processing jobs");
      offSet = offSet + PageRequest.DEFAULT_PAGE_SIZE;
    } while (accounts.size() >= PageRequest.DEFAULT_PAGE_SIZE);
    lastAvailableAccounts.removeAll(accountsFetched);
    // delete lastAvailableAccounts that are no longer available
    logger.info("Trying to Delete crons for Deleted Accounts");
    deleteCrons(lastAvailableAccounts);
    lastAvailableAccounts = accountsFetched;
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    if (!jobScheduler.checkExists(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)) {
      JobDetail job = JobBuilder.newJob(VerificationJob.class)
                          .withIdentity(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)
                          .withDescription("Verification job ")
                          .build();
      Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                              .withIntervalInSeconds((int) (CRON_POLL_INTERVAL / 2))
                                              .repeatForever())
                            .build();
      jobScheduler.scheduleJob(job, trigger);
      logger.info("Added job with details : {}", job);
    }
  }

  public void triggerDataProcessorCron(List<Account> enabledAccounts) {
    logger.info("Triggering crons for " + enabledAccounts.size() + " enabled accounts");
    enabledAccounts.forEach(account -> {
      if (verificationManagerClientHelper
              .callManagerWithRetry(verificationManagerClient.isFeatureEnabled(FeatureName.CV_24X7, account.getUuid()))
              .getResource()) {
        scheduleAPMDataProcessorCronJob(account.getUuid());
        scheduleLogDataProcessorCronJob(account.getUuid());
      }
    });
  }

  private void scheduleAPMDataProcessorCronJob(String accountId) {
    if (jobScheduler.checkExists(accountId, METRIC_DATA_PROCESSOR_CRON_GROUP)) {
      return;
    }
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
    JobDetail job = JobBuilder.newJob(MetricDataProcessorJob.class)
                        .withIdentity(accountId, METRIC_DATA_PROCESSOR_CRON_GROUP)
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("accountId", accountId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(accountId, METRIC_DATA_PROCESSOR_CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds((int) (CRON_POLL_INTERVAL / 2))
                                            .withMisfireHandlingInstructionNowWithExistingCount()
                                            .repeatForever())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled APM data collection Cron Job for Account : {}, with details : {}", accountId, job);
  }

  private void scheduleLogDataProcessorCronJob(String accountId) {
    if (jobScheduler.checkExists(accountId, LOG_DATA_PROCESSOR_CRON_GROUP)) {
      return;
    }
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
    JobDetail job = JobBuilder.newJob(LogDataProcessorJob.class)
                        .withIdentity(accountId, LOG_DATA_PROCESSOR_CRON_GROUP)
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("accountId", accountId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(accountId, LOG_DATA_PROCESSOR_CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds((int) (CRON_POLL_INTERVAL / 2))
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
      if (jobScheduler.checkExists(account.getUuid(), METRIC_DATA_PROCESSOR_CRON_GROUP)) {
        jobScheduler.deleteJob(account.getUuid(), METRIC_DATA_PROCESSOR_CRON_GROUP);
      }

      if (jobScheduler.checkExists(account.getUuid(), LOG_DATA_PROCESSOR_CRON_GROUP)) {
        jobScheduler.deleteJob(account.getUuid(), LOG_DATA_PROCESSOR_CRON_GROUP);
      }
    });
  }

  @VisibleForTesting
  public void setQuartzScheduler(PersistentScheduler jobScheduler) {
    this.jobScheduler = jobScheduler;
  }
}
