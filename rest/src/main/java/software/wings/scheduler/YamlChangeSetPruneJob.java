package software.wings.scheduler;

import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.COMPLETED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.FAILED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.SKIPPED;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class YamlChangeSetPruneJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(YamlChangeSetPruneJob.class);

  public static final String GROUP = "YAML_CHANGE_SET_PRUNE_GROUP";
  public static final String NAME = "MAINTENANCE";
  public static final String ACCOUNT_ID = "accountId";
  // Run every 6 hours, 4 times a day.
  private static final int POLL_INTERVAL = 6;
  // Delete max 20k records per job run
  private static final int MAX_DELETE_PER_JOB_RUN = 20000;
  // Delete any YamlChangeSets older than 30 days
  private static final int RETENTION_PERIOD_IN_DAYS = 30;
  // Delete 2k record in a batch
  private static final String BATCH_SIZE = "2000";

  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private AccountService accountService;
  @Inject private PersistentLocker persistentLocker;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Inject private ExecutorService executorService;

  public static void add(QuartzScheduler jobScheduler) {
    jobScheduler.deleteJob(NAME, GROUP);

    JobDetail job = JobBuilder.newJob(YamlChangeSetPruneJob.class).withIdentity(NAME, GROUP).build();

    OffsetDateTime startTime = OffsetDateTime.now().plusMinutes(10);

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(NAME, GROUP)
            .startAt(Date.from(startTime.toInstant()))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    List<Account> accounts = accountService.list(aPageRequest().addFieldsIncluded("_id").build());
    for (Account account : accounts) {
      executorService.submit(() -> executeInternal(account.getUuid()));
    }
  }

  private void executeInternal(String accountId) {
    logger.info("Running YamlChangeSetPruneJob");
    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(YamlChangeSet.class, accountId, Duration.ofSeconds(120))) {
      try {
        yamlChangeSetService.deleteChangeSets(accountId, new Status[] {FAILED, SKIPPED, COMPLETED},
            MAX_DELETE_PER_JOB_RUN, BATCH_SIZE, RETENTION_PERIOD_IN_DAYS);

      } catch (WingsException e) {
        logger.error("YamlChangeSet deletion cron job failed with error: " + e.getParams().get("message"));
      }
    } catch (Exception e) {
      logger.warn("Failed to acquire lock for account [{}]", accountId);
    }
  }
}
