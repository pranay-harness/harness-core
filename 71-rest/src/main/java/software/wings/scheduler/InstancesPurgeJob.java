package software.wings.scheduler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import io.harness.scheduler.BackgroundExecutorService;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.DateBuilder;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.TimeUnit;

@DisallowConcurrentExecution
@Slf4j
public class InstancesPurgeJob implements Job {
  private static final String INSTANCES_PURGE_CRON_NAME = "INSTANCES_PURGE_CRON_NAME";
  private static final String INSTANCES_PURGE_CRON_GROUP = "INSTANCES_PURGE_CRON_GROUP";

  private static final int MONTHS_TO_RETAIN_INSTANCES_EXCLUDING_CURRENT_MONTH = 2;
  private static final int MONTHS_TO_RETAIN_INSTANCE_STATS_EXCLUDING_CURRENT_MONTH = 5;

  @Inject private BackgroundExecutorService executorService;
  @Inject private InstanceService instanceService;
  @Inject private InstanceStatService instanceStatsService;

  public static void add(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(InstancesPurgeJob.class)
                        .withIdentity(INSTANCES_PURGE_CRON_NAME, INSTANCES_PURGE_CRON_GROUP)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(INSTANCES_PURGE_CRON_NAME, INSTANCES_PURGE_CRON_GROUP)
                          .withSchedule(CronScheduleBuilder.atHourAndMinuteOnGivenDaysOfWeek(
                              12, 0, DateBuilder.SATURDAY, DateBuilder.SUNDAY))
                          .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Triggering instances and instance stats purge job asynchronously");
    executorService.submit(this ::purge);
  }

  @VisibleForTesting
  void purge() {
    logger.info("Starting execution of instances and instance stats purge job");
    Stopwatch sw = Stopwatch.createStarted();

    purgeOldStats();
    purgeOldDeletedInstances();

    logger.info("Execution of instances and instance stats purge job completed. Time taken: {} millis",
        sw.elapsed(TimeUnit.MILLISECONDS));
  }

  private void purgeOldStats() {
    logger.info("Starting purge of instance stats");
    Stopwatch sw = Stopwatch.createStarted();

    boolean purged = instanceStatsService.purgeUpTo(getStartingInstantOfRetentionOfInstanceStats());
    if (purged) {
      logger.info(
          "Purge of instance stats completed successfully. Time taken: {} millis", sw.elapsed(TimeUnit.MILLISECONDS));
    } else {
      logger.info("Purge of instance stats failed. Time taken: {} millis", sw.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  private void purgeOldDeletedInstances() {
    logger.info("Starting purge of instances");
    Stopwatch sw = Stopwatch.createStarted();

    boolean purged = instanceService.purgeDeletedUpTo(getStartingInstantOfRetentionOfInstances());
    if (purged) {
      logger.info(
          "Purge of instances completed successfully. Time taken: {} millis", sw.elapsed(TimeUnit.MILLISECONDS));
    } else {
      logger.info("Purge of instances failed. Time taken: {} millis", sw.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  public Instant getStartingInstantOfRetentionOfInstances() {
    LocalDate firstDayOfMonthOfRetention = LocalDate.now(ZoneOffset.UTC)
                                               .minusMonths(MONTHS_TO_RETAIN_INSTANCES_EXCLUDING_CURRENT_MONTH)
                                               .with(TemporalAdjusters.firstDayOfMonth());

    return firstDayOfMonthOfRetention.atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  public Instant getStartingInstantOfRetentionOfInstanceStats() {
    LocalDate firstDayOfMonthOfRetention = LocalDate.now(ZoneOffset.UTC)
                                               .minusMonths(MONTHS_TO_RETAIN_INSTANCE_STATS_EXCLUDING_CURRENT_MONTH)
                                               .with(TemporalAdjusters.firstDayOfMonth());

    return firstDayOfMonthOfRetention.atStartOfDay().toInstant(ZoneOffset.UTC);
  }
}
