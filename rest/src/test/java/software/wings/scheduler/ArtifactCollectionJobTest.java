package software.wings.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.SchedulerException;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.exception.WingsException;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.ServiceResourceService;

import java.util.concurrent.TimeoutException;

@SetupScheduler
public class ArtifactCollectionJobTest extends WingsBaseTest {
  @Mock ServiceResourceService serviceResourceService;

  @Inject private JobScheduler jobScheduler;
  @Inject @InjectMocks ArtifactCollectionJob job = new ArtifactCollectionJob();

  private static final String appId = "Dummy App Id";
  private static final String serviceId = "Dummy service Id";
  private static final String artifactScreamId = "Dummy Artifact Stream Id";

  @Test
  public void selfPrune() throws TimeoutException, InterruptedException, SchedulerException {
    TestJobListener listener = new TestJobListener(ArtifactCollectionJob.GROUP + "." + artifactScreamId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    ArtifactCollectionJob.addDefaultJob(jobScheduler, appId, artifactScreamId);

    listener.waitToSatisfy(5000);

    assertThat(jobScheduler.deleteJob(artifactScreamId, ArtifactCollectionJob.GROUP)).isFalse();
  }

  @Test
  public void obtainServicePrunesTheService() throws SchedulerException, TimeoutException, InterruptedException {
    TestJobListener listener = new TestJobListener(PruneEntityJob.GROUP + "." + serviceId);
    jobScheduler.getScheduler().getListenerManager().addJobListener(listener);

    when(serviceResourceService.get(appId, serviceId)).thenReturn(null);

    ArtifactStream stream = new AmazonS3ArtifactStream();
    stream.setAppId(appId);
    stream.setServiceId(serviceId);
    stream.setUuid(artifactScreamId);

    assertThatThrownBy(() -> job.obtainService(appId, stream)).isInstanceOf(WingsException.class);

    listener.waitToSatisfy(5000);
  }
}
