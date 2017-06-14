package software.wings.integration;

import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
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
import software.wings.scheduler.JobScheduler;
import software.wings.scheduler.StateMachineExecutionCleanupJob;

/**
 * Created by sgurubelli on 6/13/17.
 */
@Integration
@Ignore
public class QuartzJobTriggerMigratorUtil extends WingsBaseTest {
  private static final String SM_CLEANUP_CRON_GROUP = "SM_CLEANUP_CRON_GROUP";
  private static final int SM_CLEANUP_POLL_INTERVAL = 60;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private JobScheduler jobScheduler;

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void scheduleCronForStateMachineExecutionCleanup() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);
    if (pageResponse.isEmpty() || CollectionUtils.isEmpty(pageResponse.getResponse())) {
      return;
    }
    pageResponse.getResponse().forEach(application -> {
      jobScheduler.deleteJob(application.getUuid(), "SM_CLEANUP_CRON_GROUP");
      addCronForStateMachineExecutionCleanup(application);
    });
  }

  void addCronForStateMachineExecutionCleanup(Application application) {
    JobDetail job = JobBuilder.newJob(StateMachineExecutionCleanupJob.class)
                        .withIdentity(application.getUuid(), SM_CLEANUP_CRON_GROUP)
                        .usingJobData("appId", application.getUuid())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(application.getUuid(), SM_CLEANUP_CRON_GROUP)
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(SM_CLEANUP_POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }
}
