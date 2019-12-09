package software.wings.scheduler;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.time.Duration;

@Slf4j
@Deprecated
public class WorkflowExecutionMonitorJob implements Job {
  public static final String NAME = "OBSERVER";
  public static final String GROUP = "WORKFLOW_MONITOR_CRON_GROUP";
  private static final Duration POLL_INTERVAL = Duration.ofMinutes(1);

  public static void add(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(WorkflowExecutionMonitorJob.class).withIdentity(NAME, GROUP).build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(NAME, GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInMinutes((int) POLL_INTERVAL.toMinutes())
                                            .repeatForever())
                          .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    try {
      jobExecutionContext.getScheduler().deleteJob(new JobKey(NAME, GROUP));
    } catch (Exception e) {
      logger.warn("Exception while deleting job {}-{}", NAME, GROUP);
    }
  }
}
