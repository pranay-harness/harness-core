package software.wings.scheduler;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@SetupScheduler
public class ScheduledTriggerJobTest extends WingsBaseTest {
  @Inject private BackgroundJobScheduler jobScheduler;

  private static final String accountId = "Dummy Account Id";
  private static final String appId = "Dummy App Id";
  private static final String triggerId = "Dummy Trigger Id";

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void selfPrune() throws SchedulerException, InterruptedException, TimeoutException {
    TestJobListener listener = new TestJobListener(ScheduledTriggerJob.GROUP + "." + triggerId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    final SimpleTrigger trigger = TriggerBuilder.newTrigger()
                                      .withIdentity(triggerId, ScheduledTriggerJob.GROUP)
                                      .startNow()
                                      .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                                      .build();

    ScheduledTriggerJob.add(jobScheduler, accountId, appId, triggerId, trigger);

    listener.waitToSatisfy(Duration.ofSeconds(5));

    assertThat(jobScheduler.deleteJob(triggerId, ScheduledTriggerJob.GROUP)).isFalse();
  }
}
