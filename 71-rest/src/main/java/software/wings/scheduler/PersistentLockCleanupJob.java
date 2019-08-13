package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@Slf4j
// TODO: this job is no longer needed, delete after 2019/10/15
public class PersistentLockCleanupJob implements Job {
  public static final String NAME = "MAINTENANCE";
  public static final String GROUP = "PERSISTENT_LOCK_CRON_GROUP";

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    jobScheduler.deleteJob(NAME, GROUP);
  }
}
