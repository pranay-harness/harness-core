package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.quartz.SchedulerException;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;
import software.wings.service.impl.instance.ContainerInstanceHelper;

import java.util.concurrent.TimeoutException;

@SetupScheduler
public class ContainerSyncJobTest extends WingsBaseTest {
  @Inject private JobScheduler jobScheduler;

  private static final String appId = "Dummy App Id";

  @Inject private ContainerInstanceHelper containerInstanceHelper;

  @Test
  public void selfPrune() throws SchedulerException, InterruptedException, TimeoutException {
    TestJobListener listener = new TestJobListener(ContainerSyncJob.GROUP + "." + appId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    ContainerSyncJob.add(jobScheduler, appId);

    listener.waitToSatisfy(5000);

    assertThat(jobScheduler.deleteJob(appId, ContainerSyncJob.GROUP)).isFalse();
  }
}
