package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
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
      JobScheduler jobScheduler = new JobScheduler(injector, configuration);
      addNewRelicMetricNameCollectionCron(jobScheduler);
      return jobScheduler;
    }

    private void addNewRelicMetricNameCollectionCron(JobScheduler jobScheduler) {
      try {
        if (jobScheduler.getScheduler() == null
            || jobScheduler.getScheduler()
                   .getJobKeys(GroupMatcher.anyGroup())
                   .contains(JobKey.jobKey("NEW_RELIC_METRIC_NAME_COLLECT_CRON_GROUP"))) {
          return;
        }

        Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(1));
        JobDetail job =
            JobBuilder.newJob(NewRelicMetricNameCollectionJob.class)
                .withIdentity("NEW_RELIC_METRIC_NAME_COLLECT_CRON_GROUP")
                .usingJobData("timestamp", System.currentTimeMillis())
                .withDescription(
                    "Cron to collect metric names from New Relic for workflows configured with New Relic state")
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                              .withIdentity("NEW_RELIC_METRIC_NAME_COLLECT_CRON_GROUP_TRIGGER",
                                  "NEW_RELIC_METRIC_NAME_COLLECT_CRON_GROUP_TRIGGER")
                              .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                                .withIntervalInSeconds(60)
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
