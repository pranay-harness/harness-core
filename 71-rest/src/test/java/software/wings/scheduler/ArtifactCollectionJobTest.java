package software.wings.scheduler;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.quartz.SchedulerException;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;

import java.util.concurrent.TimeoutException;

@SetupScheduler
public class ArtifactCollectionJobTest extends WingsBaseTest {
  @Inject private JobScheduler jobScheduler;
  @Inject @InjectMocks ArtifactCollectionJob job = new ArtifactCollectionJob();

  private static final String appId = "Dummy App Id";
  private static final String artifactScreamId = "Dummy Artifact Stream Id";

  @Test
  public void selfPrune() throws TimeoutException, InterruptedException, SchedulerException {
    TestJobListener listener = new TestJobListener(ArtifactCollectionJob.GROUP + "." + artifactScreamId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    ArtifactCollectionJob.addDefaultJob(jobScheduler, appId, artifactScreamId);

    listener.waitToSatisfy(ofSeconds(5));

    assertThat(jobScheduler.deleteJob(artifactScreamId, ArtifactCollectionJob.GROUP)).isFalse();
  }
}
