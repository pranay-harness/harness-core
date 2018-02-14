package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.scheduler.InstanceSyncJob;
import software.wings.scheduler.QuartzScheduler;

/**
 * @author rktummala on 09/18/17
 */
@Integration
@Ignore
public class InstanceSyncJobTriggerMigratorUtil extends WingsBaseTest {
  private static final String CONTAINER_SYNC_CRON_GROUP = "CONTAINER_SYNC_CRON_GROUP";
  // This was the old cron group name, dropping that job and adding the new cron group for all apps.
  private static final String INSTANCE_SYNC_CRON_GROUP = "INSTANCE_SYNC_CRON_GROUP";
  private static final int POLL_INTERVAL = 600;

  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void scheduleCronForInstanceSync() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    if (pageResponse.isEmpty() || isEmpty(pageResponse.getResponse())) {
      System.out.println("No applications found");
      return;
    }
    pageResponse.getResponse().forEach(application -> {
      System.out.println("Creating scheduler for application " + application);
      // deleting the old
      jobScheduler.deleteJob(application.getUuid(), INSTANCE_SYNC_CRON_GROUP);
      jobScheduler.deleteJob(application.getUuid(), CONTAINER_SYNC_CRON_GROUP);
      addCronForInstanceSync(application);
    });
  }

  void addCronForInstanceSync(Application application) {
    JobDetail job = JobBuilder.newJob(InstanceSyncJob.class)
                        .withIdentity(application.getUuid(), INSTANCE_SYNC_CRON_GROUP)
                        .usingJobData("appId", application.getUuid())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(application.getUuid(), INSTANCE_SYNC_CRON_GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }
}
