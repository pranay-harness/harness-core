package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

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
import software.wings.rules.RealMongo;
import software.wings.rules.SetupScheduler;
import software.wings.utils.Misc;

@RealMongo
@SetupScheduler
@Ignore
public class ContainerSyncJobTest extends WingsBaseTest {
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  private final static String appId = "Dummy App Id";

  public void scheduleJob() {
    JobDetail job = JobBuilder.newJob(ContainerSyncJob.class)
                        .withIdentity(appId, ContainerSyncJob.GROUP)
                        .usingJobData("appId", appId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(appId, ContainerSyncJob.GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Test
  public void selfPrune() {
    scheduleJob();
    // In 3 seconds we should have the scheduled job executed, that should discover that it is not needed and delete
    // itself.
    Misc.quietSleep(3000);
    assertThat(jobScheduler.deleteJob(appId, ContainerSyncJob.GROUP)).isFalse();
  }
}
