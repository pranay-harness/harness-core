package software.wings.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
@Slf4j
public class ExecutionLogsPruneJob implements Job {
  public static final String EXECUTION_LOGS_PRUNE_CRON_NAME = "EXECUTION_LOGS_PRUNE_CRON_NAME";
  public static final String EXECUTION_LOGS_PRUNE_CRON_GROUP = "EXECUTION_LOGS_PRUNE_CRON_GROUP";

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    // this method will be deleted once the iterator goes in
  }
}
