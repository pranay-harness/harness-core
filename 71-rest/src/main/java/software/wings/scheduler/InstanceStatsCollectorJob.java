package software.wings.scheduler;

import static software.wings.common.Constants.ACCOUNT_ID_KEY;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import io.harness.lock.AcquiredLock;
import io.harness.scheduler.BackgroundExecutorService;
import io.harness.scheduler.BackgroundSchedulerLocker;
import io.harness.scheduler.PersistentScheduler;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.instance.dashboard.InstanceStatsUtil;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.instance.stats.collector.StatsCollector;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

@DisallowConcurrentExecution
public class InstanceStatsCollectorJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(InstanceStatsCollectorJob.class);

  public static final String GROUP = "INSTANCE_STATS_COLLECT_CRON_GROUP";

  // 10 minutes
  private static final int SYNC_INTERVAL = 10;

  @Inject private BackgroundExecutorService executorService;
  @Inject private BackgroundSchedulerLocker persistentLocker;
  @Inject private StatsCollector statsCollector;
  @Inject private InstanceUsageLimitExcessHandler instanceLimitHandler;
  @Inject private InstanceStatService instanceStatService;

  public static void addWithDelay(PersistentScheduler jobScheduler, String accountId) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + new Random().nextInt((int) TimeUnit.MINUTES.toMillis(SYNC_INTERVAL));
    addInternal(jobScheduler, accountId, new Date(startTime));
  }

  public static void add(PersistentScheduler jobScheduler, String accountId) {
    addInternal(jobScheduler, accountId, null);
  }

  private static void addInternal(PersistentScheduler jobScheduler, String accountId, Date triggerStartTime) {
    JobDetail job = JobBuilder.newJob(InstanceStatsCollectorJob.class)
                        .withIdentity(accountId, GROUP)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .build();

    TriggerBuilder triggerBuilder = TriggerBuilder.newTrigger()
                                        .withIdentity(accountId, GROUP)
                                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                                          .withIntervalInMinutes(SYNC_INTERVAL)
                                                          .repeatForever()
                                                          .withMisfireHandlingInstructionNowWithExistingCount());
    if (triggerStartTime != null) {
      triggerBuilder.startAt(triggerStartTime);
    }

    jobScheduler.ensureJob__UnderConstruction(job, triggerBuilder.build());
  }

  public static void delete(PersistentScheduler jobScheduler, String accountId) {
    jobScheduler.deleteJob(accountId, GROUP);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    executorService.submit(() -> {
      String accountId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(ACCOUNT_ID_KEY);
      Objects.requireNonNull(accountId, "Account Id must be passed in job context");
      createStats(accountId);
      double ninety_five_percentile_usage = InstanceStatsUtil.actualUsage(accountId, instanceStatService);
      instanceLimitHandler.handle(accountId, ninety_five_percentile_usage);
    });
  }

  private void createStats(@Nonnull final String accountId) {
    Objects.requireNonNull(accountId, "Account Id must be present");

    try (AcquiredLock lock =
             persistentLocker.getLocker().tryToAcquireLock(Account.class, accountId, Duration.ofSeconds(120))) {
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
  }
}
