package software.wings.integration.migration.legacy;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.rules.Integration;
import software.wings.rules.SetupScheduler;
import software.wings.scheduler.AdministrativeJob;
import software.wings.scheduler.QuartzScheduler;

/**
 * Created by rsingh on 11/13/17.
 */
@Integration
@Ignore
@SetupScheduler
public class AdministrativeJobUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(AdministrativeJobUtil.class);

  private static final String ADMINISTRATIVE_CRON_NAME = "ADMINISTRATIVE_CRON_NAME";
  private static final String ADMINISTRATIVE_CRON_GROUP = "ADMINISTRATIVE_CRON_GROUP";

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void addCronForAdministrativeJob() {
    logger.info("Adding Administrative cron");
    JobDetail job = JobBuilder.newJob(AdministrativeJob.class)
                        .withIdentity(ADMINISTRATIVE_CRON_NAME, ADMINISTRATIVE_CRON_GROUP)
                        .withDescription("Administrative job ")
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(ADMINISTRATIVE_CRON_NAME, ADMINISTRATIVE_CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(3).repeatForever())
                          .build();
    jobScheduler.scheduleJob(job, trigger);
    logger.info("Added Administrative cron");
  }

  @Test
  public void deleteCronForAdministrativeJob() {
    jobScheduler.deleteJob(ADMINISTRATIVE_CRON_NAME, ADMINISTRATIVE_CRON_GROUP);
  }
}
