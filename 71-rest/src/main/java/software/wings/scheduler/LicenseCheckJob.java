package software.wings.scheduler;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@DisallowConcurrentExecution
@Slf4j
// TODO: Class to be deleted by 11/30/2019
public class LicenseCheckJob implements Job {
  private static final String CRON_NAME = "LICENSE_CHECK_CRON_NAME";
  private static final String CRON_GROUP = "LICENSE_CHECK_CRON_GROUP";

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    // Do nothing
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    try {
      if (jobScheduler.checkExists(CRON_NAME, CRON_GROUP)) {
        jobScheduler.deleteJob(CRON_NAME, CRON_GROUP);
      }
    } catch (Exception ex) {
      logger.warn("Exception while deleting job {}", CRON_NAME);
    }
  }
}
