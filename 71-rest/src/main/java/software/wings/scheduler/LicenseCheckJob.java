package software.wings.scheduler;

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
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.licensing.LicenseService;

import java.time.Duration;

@DisallowConcurrentExecution
public class LicenseCheckJob implements Job {
  private static final String CRON_NAME = "LICENSE_CHECK_CRON_NAME";
  private static final String CRON_GROUP = "LICENSE_CHECK_CRON_GROUP";
  private static final String LOCK = "LICENSE_CHECK";
  private static final Logger logger = LoggerFactory.getLogger(LicenseCheckJob.class);

  @Inject private LicenseService licenseManager;
  @Inject private BackgroundExecutorService executorService;
  @Inject private BackgroundSchedulerLocker persistentLocker;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running license check job asynchronously and returning");
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    try (AcquiredLock lock = persistentLocker.getLocker().tryToAcquireLock(LOCK, Duration.ofMinutes(5))) {
      if (lock == null) {
        return;
      }

      logger.info("Running license check job");
      licenseManager.checkForLicenseExpiry();
      logger.info("License check job complete");
    }
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(LicenseCheckJob.class)
                        .withIdentity(CRON_NAME, CRON_GROUP)
                        .withDescription("Administrative job ")
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(CRON_NAME, CRON_GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(30).repeatForever())
            .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }
}
