package software.wings.scheduler;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
// TODO- delete class on 11/30/2019
@DisallowConcurrentExecution
@Slf4j
public class InstanceStatsMetricsJob implements Job {
  private static final String CRON_NAME = "INSTANCE_STATS_METRICS_CRON_NAME";
  private static final String CRON_GROUP = "INSTANCE_STATS_METRICS_CRON_GROUP";

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {}

  public static void addJob(PersistentScheduler jobScheduler) {
    if (jobScheduler.checkExists(CRON_NAME, CRON_GROUP)) {
      jobScheduler.deleteJob(CRON_NAME, CRON_GROUP);
    }
  }
}
