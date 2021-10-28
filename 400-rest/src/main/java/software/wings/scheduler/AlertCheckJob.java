package software.wings.scheduler;

import static software.wings.common.Constants.ACCOUNT_ID_KEY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.dms.DmsProxy;
import io.harness.scheduler.PersistentScheduler;

import com.google.inject.Inject;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;

/**
 * @author brett on 10/17/17
 */
@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._360_CG_MANAGER)
public class AlertCheckJob implements Job {
  private static final SecureRandom random = new SecureRandom();
  public static final String GROUP = "ALERT_CHECK_CRON_GROUP";

  public static final int POLL_INTERVAL = 300;
  public static final long MAX_HB_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

  @Inject private ExecutorService executorService;
  @Inject private DmsProxy dmsProxy;

  public static void addWithDelay(PersistentScheduler jobScheduler, String accountId) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + random.nextInt((int) TimeUnit.SECONDS.toMillis(POLL_INTERVAL));
    addInternal(jobScheduler, accountId, new Date(startTime));
  }

  public static void add(PersistentScheduler jobScheduler, String accountId) {
    addInternal(jobScheduler, accountId, null);
  }

  private static void addInternal(PersistentScheduler jobScheduler, String accountId, Date triggerStartTime) {
    JobDetail job = JobBuilder.newJob(AlertCheckJob.class)
                        .withIdentity(accountId, GROUP)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .build();

    TriggerBuilder triggerBuilder =
        TriggerBuilder.newTrigger()
            .withIdentity(accountId, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever());
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
    executorService.submit(()
                               -> dmsProxy.alertCheckJobExecute(
                                   (String) jobExecutionContext.getJobDetail().getJobDataMap().get(ACCOUNT_ID_KEY)));
  }
}
