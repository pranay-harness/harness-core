package io.harness.jobs;

import static io.harness.app.VerificationServiceApplication.CRON_POLL_INTERVAL;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
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
import software.wings.scheduler.QuartzScheduler;

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

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
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
      triggerDataProcessorCron(enabledAccounts, JobExecutionContext);
      logger.info("Completed scheduling APM and Log processing jobs");
      offSet = offSet + PageRequest.DEFAULT_PAGE_SIZE;
    } while (accounts.size() >= PageRequest.DEFAULT_PAGE_SIZE);
    lastAvailableAccounts.removeAll(accountsFetched);
    // delete lastAvailableAccounts that are no longer available
    logger.info("Trying to Delete crons for Deleted Accounts");
    deleteCrons(lastAvailableAccounts);
    lastAvailableAccounts = accountsFetched;
  }

  public static void addJob(QuartzScheduler jobScheduler) {
    if (!jobScheduler.checkExists(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)) {
      JobDetail job = JobBuilder.newJob(VerificationJob.class)
                          .withIdentity(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)
                          .withDescription("Verification job ")
                          .build();
      Trigger trigger =
          TriggerBuilder.newTrigger()
              .withIdentity(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)
              .withSchedule(
                  SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(CRON_POLL_INTERVAL / 2).repeatForever())
              .build();
      jobScheduler.scheduleJob(job, trigger);
      logger.info("Added job with details : {}", job);
    }
  }

  public void triggerDataProcessorCron(List<Account> enabledAccounts, JobExecutionContext jobExecutionContext) {
    logger.info("Triggering crons for " + enabledAccounts.size() + " enabled accounts");
    enabledAccounts.forEach(account -> {
      scheduleAPMDataProcessorCronJob(account.getUuid(), jobExecutionContext);
      scheduleLogDataProcessorCronJob(account.getUuid(), jobExecutionContext);
    });
  }

  private void scheduleAPMDataProcessorCronJob(String accountId, JobExecutionContext jobExecutionContext) {
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
    JobDetail job =
        JobBuilder.newJob(MetricDataProcessorJob.class).usingJobData("timestamp", System.currentTimeMillis()).build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(accountId, MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(CRON_POLL_INTERVAL / 2)
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled APM data collection Cron Job for Account : {}, with details : {}", accountId, job);
  }

  private void scheduleLogDataProcessorCronJob(String accountId, JobExecutionContext jobExecutionContext) {
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
    JobDetail job =
        JobBuilder.newJob(LogDataProcessorJob.class).usingJobData("timestamp", System.currentTimeMillis()).build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(accountId, LogDataProcessorJob.LOG_DATA_PROCESSOR_CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(CRON_POLL_INTERVAL / 2)
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled Log data collection Cron Job for Account : {}, with details : {}", accountId, job);
  }

  public void deleteCrons(List<Account> disabledAccounts) {
    logger.info("Deleting crons for " + disabledAccounts.size() + " accounts");
    disabledAccounts.forEach(account -> {
      if (jobScheduler.checkExists(account.getUuid(), MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP)) {
        jobScheduler.deleteJob(account.getUuid(), MetricDataProcessorJob.METRIC_DATA_PROCESSOR_CRON_GROUP);
      }

      if (jobScheduler.checkExists(account.getUuid(), LogDataProcessorJob.LOG_DATA_PROCESSOR_CRON_GROUP)) {
        jobScheduler.deleteJob(account.getUuid(), LogDataProcessorJob.LOG_DATA_PROCESSOR_CRON_GROUP);
      }
    });
  }

  @VisibleForTesting
  public void setQuartzScheduler(QuartzScheduler jobScheduler) {
    this.jobScheduler = jobScheduler;
  }
}
