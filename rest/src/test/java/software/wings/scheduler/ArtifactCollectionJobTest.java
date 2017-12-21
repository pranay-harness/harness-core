package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.quartz.SchedulerException;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;

import java.util.concurrent.TimeoutException;

@SetupScheduler
public class ArtifactCollectionJobTest extends WingsBaseTest {
  @Inject private JobScheduler jobScheduler;

  private final static String appId = "Dummy App Id";
  private final static String artifactSreamId = "Dummy Artifact Stream Id";

  @Test
  public void selfPrune() throws TimeoutException, InterruptedException, SchedulerException {
    TestJobListener listener = new TestJobListener(ArtifactCollectionJob.GROUP + "." + artifactSreamId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    ArtifactCollectionJob.addDefaultJob(jobScheduler, appId, artifactSreamId);

    listener.waitToSatisfy(5000);

    assertThat(jobScheduler.deleteJob(artifactSreamId, ArtifactCollectionJob.GROUP)).isFalse();
  }
}
