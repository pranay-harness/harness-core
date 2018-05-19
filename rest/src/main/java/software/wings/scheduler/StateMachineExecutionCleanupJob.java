package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionInterruptManager;

// TODO: Remove this job after it cleans all it instances
public class StateMachineExecutionCleanupJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(StateMachineExecutionCleanupJob.class);

  public static final String GROUP = "SM_CLEANUP_CRON_GROUP";
  private static final int POLL_INTERVAL = 60;

  public static final String APP_ID_KEY = "appId";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionInterruptManager executionInterruptManager;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
    jobScheduler.deleteJob(appId, GROUP);
  }
}
