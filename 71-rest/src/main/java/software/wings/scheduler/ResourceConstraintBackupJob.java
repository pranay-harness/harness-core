package software.wings.scheduler;

import com.google.inject.Inject;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.service.intfc.ResourceConstraintService;

/*
This job is migrated to use Iterators
io.harness.workers.background.critical.iterator.ResourceConstraintBackupHandler
 */
@Slf4j
@Deprecated
public class ResourceConstraintBackupJob implements Job {
  public static final String NAME = "BACKUP";
  public static final String GROUP = "RESOURCE_CONSTRAINT_GROUP";
  private static final int POLL_INTERVAL = 60;

  @Inject private ResourceConstraintService resourceConstraintService;

  public static Trigger trigger() {
    return TriggerBuilder.newTrigger()
        .withIdentity(NAME, GROUP)
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
        .build();
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    JobDetail details = JobBuilder.newJob(ResourceConstraintBackupJob.class).withIdentity(NAME, GROUP).build();
    jobScheduler.ensureJob__UnderConstruction(details, trigger());
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      jobExecutionContext.getScheduler().deleteJob(new JobKey(NAME, GROUP));
    } catch (Exception ex) {
      logger.warn("Exception while deleting job {}-{}", NAME, GROUP);
    }
  }
}
