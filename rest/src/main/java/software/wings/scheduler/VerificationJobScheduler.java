package software.wings.scheduler;

import static org.quartz.JobKey.jobKey;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class VerificationJobScheduler extends AbstractQuartzScheduler {
  private static final Logger logger = LoggerFactory.getLogger(VerificationJobScheduler.class);
  private static final int DEFAULT_COLLECTION_MINS = 60;
  private static final int DEFAULT_CLEANUP_MINS = 1;

  /**
   * Instantiates a new Cron scheduler.
   *
   * @param injector      the injector
   * @param configuration the configuration
   */
  private VerificationJobScheduler(Injector injector, MainConfiguration configuration) {
    super(injector, configuration);
  }

  public static class JobSchedulerProvider implements Provider<JobScheduler> {
    @Inject Injector injector;
    @Inject MainConfiguration configuration;

    @Override
    public JobScheduler get() {
      configuration.getSchedulerConfig().setSchedulerName("verification_scheduler");
      configuration.getSchedulerConfig().setInstanceId("verification");
      configuration.getSchedulerConfig().setTablePrefix("quartz_verification");
      configuration.getSchedulerConfig().setThreadCount("15");
      //      configuration.getSchedulerConfig().setClustered(false);
      JobScheduler jobScheduler = new JobScheduler(injector, configuration);
      addLearningEngineCleaupJobCron(jobScheduler);
      return jobScheduler;
    }

    private void addLearningEngineCleaupJobCron(JobScheduler jobScheduler) {
      try {
        if (jobScheduler.getScheduler() == null
            || jobScheduler.getScheduler()
                   .getJobKeys(GroupMatcher.anyGroup())
                   .contains(
                       jobKey("LEARNING_ENGINE_TASK_QUEUE_DEL_CRON", "LEARNING_ENGINE_TASK_QUEUE_DEL_CRON_GROUP"))) {
          return;
        }

        Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
        JobDetail job =
            JobBuilder.newJob(LearningEngineTaskQueueCleanUpJob.class)
                .withIdentity("LEARNING_ENGINE_TASK_QUEUE_DEL_CRON", "LEARNING_ENGINE_TASK_QUEUE_DEL_CRON_GROUP")
                .usingJobData("timestamp", System.currentTimeMillis())
                .withDescription("Cron to delete learning engine tasks older than 7 days")
                .build();

        Trigger trigger =
            TriggerBuilder.newTrigger()
                .withIdentity("LEARNING_ENGINE_TASK_QUEUE_DEL_CRON", "LEARNING_ENGINE_TASK_QUEUE_DEL_CRON_GROUP")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                  .withIntervalInSeconds((int) TimeUnit.MINUTES.toSeconds(DEFAULT_CLEANUP_MINS))
                                  .withMisfireHandlingInstructionNowWithExistingCount()
                                  .repeatForever())
                .startAt(startDate)
                .build();

        jobScheduler.scheduleJob(job, trigger);
      } catch (SchedulerException e) {
        logger.error("Unable to start new relic metric names cron", e);
      }
    }
  }
}
