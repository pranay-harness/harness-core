package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.WingsBaseTest;
import software.wings.rules.RealMongo;
import software.wings.rules.SetupScheduler;

@SetupScheduler
@Ignore
public class StateMachineExecutionCleanupJobTest extends WingsBaseTest {
  @Inject private JobScheduler jobScheduler;

  private final static String appId = "Dummy App Id";

  public void scheduleJob() {
    JobDetail job = JobBuilder.newJob(StateMachineExecutionCleanupJob.class)
                        .withIdentity(appId, StateMachineExecutionCleanupJob.GROUP)
                        .usingJobData(StateMachineExecutionCleanupJob.APP_ID_KEY, appId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(appId, StateMachineExecutionCleanupJob.GROUP)
                          .startNow()
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
