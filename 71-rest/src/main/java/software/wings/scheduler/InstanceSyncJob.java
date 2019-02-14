package software.wings.scheduler;

import static java.lang.String.format;
import static software.wings.common.Constants.ACCOUNT_ID_KEY;
import static software.wings.common.Constants.APP_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Periodic job that syncs for instances for all the deployment types except Physical data center.
 *
 * @author rktummala on 09/14/17
 */
public class InstanceSyncJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(InstanceSyncJob.class);

  public static final String GROUP = "INSTANCE_SYNC_CRON_GROUP";
  private static final int POLL_INTERVAL = 600;

  @Inject @Named("ServiceJobScheduler") private PersistentScheduler jobScheduler;

  public static void addWithDelay(PersistentScheduler jobScheduler, String accountId, String appId) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + new Random().nextInt((int) TimeUnit.SECONDS.toMillis(POLL_INTERVAL));
    addInternal(jobScheduler, accountId, appId, new Date(startTime));
  }

  public static void add(PersistentScheduler jobScheduler, String accountId, String appId) {
    addInternal(jobScheduler, accountId, appId, null);
  }

  private static void addInternal(
      PersistentScheduler jobScheduler, String accountId, String appId, Date triggerStartTime) {
    JobDetail job = JobBuilder.newJob(InstanceSyncJob.class)
                        .withIdentity(appId, GROUP)
                        .usingJobData(APP_ID_KEY, appId)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .build();

    TriggerBuilder triggerBuilder = TriggerBuilder.newTrigger()
                                        .withIdentity(appId, GROUP)
                                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                                          .withIntervalInSeconds(POLL_INTERVAL)
                                                          .repeatForever()
                                                          .withMisfireHandlingInstructionNowWithExistingCount());
    if (triggerStartTime != null) {
      triggerBuilder.startAt(triggerStartTime);
    }

    jobScheduler.ensureJob__UnderConstruction(job, triggerBuilder.build());
  }

  public static void delete(PersistentScheduler jobScheduler, String appId) {
    jobScheduler.deleteJob(appId, GROUP);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String appId = null;
    try {
      appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
      delete(jobScheduler, appId);
    } catch (Exception ex) {
      logger.warn(format("Error while deleting instance sync job for appId %s", appId), ex);
    }
  }
}
