package software.wings.scheduler;

import com.google.inject.Inject;

import io.harness.scheduler.PersistentScheduler;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.DataStoreService;

public class ExecutionLogsPruneJob implements Job {
  private static final String EXECUTION_LOGS_PRUNE_CRON_NAME = "EXECUTION_LOGS_PRUNE_CRON_NAME";
  private static final String EXECUTION_LOGS_PRUNE_CRON_GROUP = "EXECUTION_LOGS_PRUNE_CRON_GROUP";
  private static final Logger logger = LoggerFactory.getLogger(ExecutionLogsPruneJob.class);

  @Inject private DataStoreService dataStoreService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running execution logs cleanup Job");
    dataStoreService.purgeOlderRecords();
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(ExecutionLogsPruneJob.class)
                        .withIdentity(EXECUTION_LOGS_PRUNE_CRON_NAME, EXECUTION_LOGS_PRUNE_CRON_GROUP)
                        .withDescription("Execution Logs prune job ")
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(EXECUTION_LOGS_PRUNE_CRON_NAME, EXECUTION_LOGS_PRUNE_CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1).repeatForever())
                          .build();
    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }
}
