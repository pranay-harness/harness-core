package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.WingsBaseTest;
import software.wings.rules.Integration;
import software.wings.rules.SetupScheduler;
import software.wings.utils.Misc;

@Integration
@SetupScheduler
public class StateMachineExecutionCleanupJobTest extends WingsBaseTest {
  @Inject private JobScheduler jobScheduler;

  private final static String appId = "Dummy App Id";

  public void scheduleJob() {
    JobDetail job = JobBuilder.newJob(StateMachineExecutionCleanupJob.class)
                        .withIdentity(appId, StateMachineExecutionCleanupJob.GROUP)
                        .usingJobData("appId", appId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(appId, StateMachineExecutionCleanupJob.GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(1).repeatForever())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Test
  public void selfPrune() throws SchedulerException, InterruptedException {
    TestJobListener listener = new TestJobListener(StateMachineExecutionCleanupJob.GROUP + "." + appId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    scheduleJob();

    synchronized (listener) {
      listener.wait(5000);
    }

    assertThat(jobScheduler.deleteJob(appId, StateMachineExecutionCleanupJob.GROUP)).isFalse();
  }
}
