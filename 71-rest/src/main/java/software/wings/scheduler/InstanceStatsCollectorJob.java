package software.wings.scheduler;

import static software.wings.common.Constants.ACCOUNT_ID_KEY;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
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
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.resources.DashboardStatisticsResource;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.instance.stats.collector.StatsCollector;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
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
  @Inject private DashboardStatisticsService dashboardStatisticsService;
  @Inject private AccountService accountService;
  @Inject private UsageMetricsEventPublisher eventPublisher;

  public static void add(PersistentScheduler jobScheduler, String accountId) {
    JobDetail job = JobBuilder.newJob(InstanceStatsCollectorJob.class)
                        .withIdentity(accountId, GROUP)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(accountId, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(SYNC_INTERVAL).repeatForever())
            .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
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
      double ninety_five_percentile_usage = actualUsage(accountId);
      instanceLimitHandler.handle(accountId, ninety_five_percentile_usage);
      try {
        int currentInstanceCount =
            dashboardStatisticsService.getAppInstancesForAccount(accountId, System.currentTimeMillis()).size();
        Account accountWithDefaults = accountService.getAccountWithDefaults(accountId);
        eventPublisher.publishInstanceMetric(accountId, accountWithDefaults.getAccountName(),
            ninety_five_percentile_usage, EventConstants.INSTANCE_COUNT_NINETY_FIVE_PERCENTILE);
        eventPublisher.publishInstanceMetric(
            accountId, accountWithDefaults.getAccountName(), currentInstanceCount, EventConstants.INSTANCE_COUNT_TOTAL);
      } catch (Exception e) {
        logger.warn("Failed to publish eventType:[{}] for accountID:[{}]", EventType.INSTANCE_COUNT, accountId, e);
      }
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

  // Find 95th percentile of usage in last 30 days.
  // This will serve as 'average' usage in last 30 days (the word 'average' is not used mathematically here)
  private double actualUsage(String accountId) {
    Instant now = Instant.now();
    Instant from = now.minus(30, ChronoUnit.DAYS);
    return instanceStatService.percentile(accountId, from, now, DashboardStatisticsResource.DEFAULT_PERCENTILE);
  }
}
