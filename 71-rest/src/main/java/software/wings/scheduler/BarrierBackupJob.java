package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
// TODO: this job is no longer needed, delete after 2019/09/09
public class BarrierBackupJob implements Job {
  public static final String NAME = "BACKUP";
  public static final String GROUP = "BARRIER_GROUP";

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    jobScheduler.deleteJob(NAME, GROUP);
  }
}
