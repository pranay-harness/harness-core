package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;

import java.util.concurrent.TimeoutException;

@SetupScheduler
public class ScheduledTriggerJobTest extends WingsBaseTest {
  @Inject private JobScheduler jobScheduler;

  private final static String appId = "Dummy App Id";
  private final static String triggerId = "Dummy Trigger Id";

  @Test
  public void selfPrune() throws SchedulerException, InterruptedException, TimeoutException {
    TestJobListener listener = new TestJobListener(ScheduledTriggerJob.GROUP + "." + triggerId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    final SimpleTrigger trigger = TriggerBuilder.newTrigger()
                                      .withIdentity(triggerId, ScheduledTriggerJob.GROUP)
                                      .startNow()
                                      .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                                      .build();

    ScheduledTriggerJob.add(jobScheduler, appId, triggerId, trigger);

    listener.waitToSatisfy(5000);

    assertThat(jobScheduler.deleteJob(triggerId, ScheduledTriggerJob.GROUP)).isFalse();
  }
}
