package software.wings.scheduler;

import com.google.inject.Inject;

import io.harness.event.usagemetrics.UsageMetricsService;
import io.harness.lock.AcquiredLock;
import io.harness.scheduler.BackgroundExecutorService;
import io.harness.scheduler.BackgroundSchedulerLocker;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.time.Duration;

@DisallowConcurrentExecution
@Slf4j
public class UsageMetricsJob implements Job {
  private static final String CRON_NAME = "USAGE_METRICS_CRON_NAME";
  private static final String CRON_GROUP = "USAGE_METRICS_CRON_GROUP";
  private static final String LOCK = "USAGE_METRICS";

  @Inject private BackgroundExecutorService executorService;
  @Inject private BackgroundSchedulerLocker persistentLocker;
  @Inject private UsageMetricsService usageMetricsService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running usage metrics job asynchronously");
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    try (AcquiredLock lock = persistentLocker.getLocker().tryToAcquireLock(LOCK, Duration.ofMinutes(5))) {
      if (lock == null) {
        return;
      }

      logger.info("Running usage metrics job");
      usageMetricsService.checkUsageMetrics();
      logger.info("Usage metrics job complete");
    }
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(UsageMetricsJob.class)
                        .withIdentity(CRON_NAME, CRON_GROUP)
                        .withDescription("Administrative job ")
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(CRON_NAME, CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(4).repeatForever())
                          .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }
}
