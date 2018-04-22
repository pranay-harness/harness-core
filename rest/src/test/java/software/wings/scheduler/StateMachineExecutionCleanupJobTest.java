package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.quartz.SchedulerException;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;

import java.util.concurrent.TimeoutException;

@SetupScheduler
public class StateMachineExecutionCleanupJobTest extends WingsBaseTest {
  @Inject private JobScheduler jobScheduler;

  @Inject StateMachineExecutionCleanupJob job;

  private static final String appId = "Dummy App Id";

  @Test
  public void selfPrune() throws SchedulerException, InterruptedException, TimeoutException {
    TestJobListener listener = new TestJobListener(StateMachineExecutionCleanupJob.GROUP + "." + appId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    StateMachineExecutionCleanupJob.add(jobScheduler, appId);

    listener.waitToSatisfy(5000);

    assertThat(jobScheduler.deleteJob(appId, StateMachineExecutionCleanupJob.GROUP)).isFalse();
  }
}
