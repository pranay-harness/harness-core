package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.SchedulerException;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@SetupScheduler
public class InstanceSyncJobTest extends WingsBaseTest {
  @Inject private ServiceJobScheduler jobScheduler;

  private static final String accountId = "Dummy Account Id";
  private static final String appId = "Dummy App Id";

  @Test
  @Category(UnitTests.class)
  public void selfPrune() throws SchedulerException, InterruptedException, TimeoutException {
    TestJobListener listener = new TestJobListener(InstanceSyncJob.GROUP + "." + appId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    InstanceSyncJob.add(jobScheduler, accountId, appId);

    listener.waitToSatisfy(Duration.ofSeconds(5));

    assertThat(jobScheduler.deleteJob(appId, InstanceSyncJob.GROUP)).isFalse();
  }
}
