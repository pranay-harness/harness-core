package software.wings.scheduler;

import com.google.inject.Inject;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.security.SecretManager;

import java.util.concurrent.ExecutorService;

public class AdministrativeJob implements Job {
  private static final String ADMINISTRATIVE_CRON_NAME = "ADMINISTRATIVE_CRON_NAME";
  private static final String ADMINISTRATIVE_CRON_GROUP = "ADMINISTRATIVE_CRON_GROUP";
  private static final Logger logger = LoggerFactory.getLogger(AdministrativeJob.class);

  @Inject private SecretManager secretManager;
  @Inject private ExecutorService executorService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running Administrative Job asynchronously and returning");
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    logger.info("Running Administrative Job");
    secretManager.checkAndAlertForInvalidManagers();
    logger.info("Administrative Job complete");
  }

  public static void addJob(QuartzScheduler jobScheduler) {
    jobScheduler.deleteJob(ADMINISTRATIVE_CRON_NAME, ADMINISTRATIVE_CRON_GROUP);
    JobDetail job = JobBuilder.newJob(AdministrativeJob.class)
                        .withIdentity(ADMINISTRATIVE_CRON_NAME, ADMINISTRATIVE_CRON_GROUP)
                        .withDescription("Administrative job ")
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(ADMINISTRATIVE_CRON_NAME, ADMINISTRATIVE_CRON_GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(10).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }
}
