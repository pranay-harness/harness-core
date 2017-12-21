package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.instance.ContainerInstanceHelper;

import java.util.concurrent.TimeoutException;

@SetupScheduler
public class ContainerSyncJobTest extends WingsBaseTest {
  @Inject private JobScheduler jobScheduler;

  private final static String appId = "Dummy App Id";

  @Inject private ContainerInstanceHelper containerInstanceHelper;

  public void scheduleJob() {
    JobDetail job = JobBuilder.newJob(ContainerSyncJob.class)
                        .withIdentity(appId, ContainerSyncJob.GROUP)
                        .usingJobData(ContainerSyncJob.APP_ID_KEY, appId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(appId, ContainerSyncJob.GROUP)
                          .startNow()
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Test
  public void selfPrune() throws SchedulerException, InterruptedException, TimeoutException {
    TestJobListener listener = new TestJobListener(ContainerSyncJob.GROUP + "." + appId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    scheduleJob();

    listener.waitToSatisfy(5000);

    assertThat(jobScheduler.deleteJob(appId, ContainerSyncJob.GROUP)).isFalse();
  }
}
