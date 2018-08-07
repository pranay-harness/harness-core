package software.wings.scheduler;

import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.BarrierService;

public class BarrierBackupJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(BarrierBackupJob.class);

  public static final String NAME = "BACKUP";
  public static final String GROUP = "BARRIER_GROUP";
  private static final int POLL_INTERVAL = 60;

  @Inject private BarrierService barrierService;

  public static Trigger trigger() {
    return TriggerBuilder.newTrigger()
        .withIdentity(NAME, GROUP)
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
        .build();
  }

  public static void addJob(QuartzScheduler jobScheduler) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(NAME, GROUP);

    JobDetail details = JobBuilder.newJob(BarrierBackupJob.class).withIdentity(NAME, GROUP).build();
    jobScheduler.scheduleJob(details, trigger());
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      barrierService.updateAllActiveBarriers();
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException e) {
      logger.error("", e);
    }
  }
}
