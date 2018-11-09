package software.wings.scheduler;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.scheduler.PersistentScheduler;
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
import software.wings.service.intfc.instance.stats.collector.StatsCollector;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author rktummala on 10/08/18
 */
public class InstanceStatsCollectorJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(InstanceStatsCollectorJob.class);

  public static final String GROUP = "INSTANCE_STATS_COLLECT_CRON_GROUP";
  public static final String ACCOUNT_ID = "accountId";

  // 10 minutes
  private static final int SYNC_INTERVAL = 10;

  @Inject private ExecutorService executorService;
  @Inject private StatsCollector statsCollector;
  @Inject private PersistentLocker persistentLocker;

  public static void add(PersistentScheduler jobScheduler, Account account) {
    jobScheduler.deleteJob(account.getUuid(), GROUP);
    JobDetail job = JobBuilder.newJob(InstanceStatsCollectorJob.class)
                        .withIdentity(account.getUuid(), GROUP)
                        .usingJobData(ACCOUNT_ID, account.getUuid())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(account.getUuid(), GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(SYNC_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  public static void delete(PersistentScheduler jobScheduler, String accountId) {
    jobScheduler.deleteJob(accountId, GROUP);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    executorService.submit(() -> {
      String accountId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(ACCOUNT_ID);
      Objects.requireNonNull(accountId, "Account Id must be passed in job context");

      try (AcquiredLock lock = persistentLocker.tryToAcquireLock(Account.class, accountId, Duration.ofSeconds(120))) {
        if (lock == null) {
          return;
        }

        Stopwatch sw = Stopwatch.createStarted();
        boolean ranAtLeastOnce = statsCollector.createStats(accountId);
        if (ranAtLeastOnce) {
          logger.info("Successfully saved instance history stats. Account Id: {}. Time taken: {} millis", accountId,
              sw.elapsed(TimeUnit.MILLISECONDS));
        } else {
          logger.info("No instance history stats were saved. Account Id: {}. Time taken: {} millis", accountId,
              sw.elapsed(TimeUnit.MILLISECONDS));
        }
      }
    });
  }
}
