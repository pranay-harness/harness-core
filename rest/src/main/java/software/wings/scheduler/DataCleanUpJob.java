package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * Cron that runs every mid night to cleanup the data
 * Created by sgurubelli on 7/19/17.
 */
public class DataCleanUpJob implements Job {
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    jobScheduler.deleteJob("DATA_CLEANUP_CRON_NAME", "DATA_CLEANUP_CRON_GROUP");
  }
}
