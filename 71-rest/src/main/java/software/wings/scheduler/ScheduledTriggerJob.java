package software.wings.scheduler;

import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.TriggerService;

/**
 * Created by sgurubelli on 10/26/17.
 */
public class ScheduledTriggerJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(ScheduledTriggerJob.class);

  public static final String GROUP = "SCHEDULED_TRIGGER_CRON_GROUP";

  // 'Second' unit prefix to convert unix to quartz cron expression
  public static final String PREFIX = "0 ";
  public static final String TRIGGER_ID_KEY = "triggerId";
  public static final String APP_ID_KEY = "appId";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private TriggerService triggerService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  public static org.quartz.Trigger getQuartzTrigger(Trigger trigger) {
    return TriggerBuilder.newTrigger()
        .withIdentity(trigger.getUuid(), ScheduledTriggerJob.GROUP)
        .withSchedule(CronScheduleBuilder.cronSchedule(
            PREFIX + ((ScheduledTriggerCondition) trigger.getCondition()).getCronExpression()))
        .build();
  }

  public static void add(PersistentScheduler jobScheduler, String appId, String triggerId, org.quartz.Trigger trigger) {
    JobDetail job = JobBuilder.newJob(ScheduledTriggerJob.class)
                        .withIdentity(triggerId, ScheduledTriggerJob.GROUP)
                        .usingJobData(TRIGGER_ID_KEY, triggerId)
                        .usingJobData(APP_ID_KEY, appId)
                        .build();
    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String triggerId = jobExecutionContext.getMergedJobDataMap().getString(TRIGGER_ID_KEY);
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);

    Trigger trigger = wingsPersistence.get(Trigger.class, appId, triggerId);
    if (trigger == null || !trigger.getCondition().getConditionType().equals(SCHEDULED)) {
      logger.info("Trigger not found or wrong type. Deleting job associated to it");
      jobScheduler.deleteJob(triggerId, GROUP);
      return;
    }
    logger.info("Triggering scheduled job for appId {} and triggerId {} with the scheduled fire time {}", appId,
        triggerId, jobExecutionContext.getNextFireTime());
    triggerService.triggerScheduledExecutionAsync(trigger, jobExecutionContext.getNextFireTime());
  }
}
