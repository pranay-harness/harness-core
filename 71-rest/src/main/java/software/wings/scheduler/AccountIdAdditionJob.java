package software.wings.scheduler;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * @author rktummala on 03/05/19
 */
@DisallowConcurrentExecution
@Slf4j
// TODO: Class to be deleted by 11/30/2019
public class AccountIdAdditionJob implements Job {
  private static final String CRON_NAME = "ACCOUNT_ID_ADDITION_CRON_NAME";
  private static final String CRON_GROUP = "ACCOUNT_ID_ADDITION_CRON_GROUP";

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    // do nothing
  }

  public static void delete(PersistentScheduler jobScheduler) {
    try {
      if (jobScheduler.checkExists(CRON_NAME, CRON_GROUP)) {
        jobScheduler.deleteJob(CRON_NAME, CRON_GROUP);
      }
    } catch (Exception ex) {
      logger.warn("Exception while deleting job {}", CRON_NAME);
    }
  }
}
