package software.wings.collect;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactSource;
import static software.wings.collect.CollectEvent.Builder.aCollectEvent;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_ID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ApprovalNotification;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.NotificationService;

import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
public class ArtifactCollectEventListenerTest extends WingsBaseTest {
  /**
   * The constant ARTIFACT_FILE.
   */
  public static final ArtifactFile ARTIFACT_FILE =
      anArtifactFile().withAppId(APP_ID).withUuid("ARTIFACT_FILE_ID").build();

  /**
   * The constant ARTIFACT_SOURCE_NAME.
   */
  public static final String ARTIFACT_SOURCE_NAME = "job1";
  private final ArtifactStream ARTIFACT_SOURCE = aJenkinsArtifactSource().withSourceName(ARTIFACT_SOURCE_NAME).build();

  @InjectMocks @Inject private ArtifactCollectEventListener artifactCollectEventListener;

  @Mock private ArtifactService artifactService;

  @Mock private Map<String, ArtifactCollectorService> collectorServiceMap;

  @Mock private ArtifactCollectorService artifactCollectorService;

  @Mock private NotificationService notificationService;

  @Mock private ArtifactStreamService artifactStreamService;

  /**
   * The Verifier.
   */
  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(artifactService, collectorServiceMap, artifactCollectorService, notificationService);
    }
  };

  /**
   * Setup mocks.
   */
  @Before
  public void setupMocks() {
    when(collectorServiceMap.get(anyString())).thenReturn(artifactCollectorService);
    when(artifactStreamService.get(ARTIFACT_SOURCE_ID, APP_ID)).thenReturn(ARTIFACT_SOURCE);
  }

  /**
   * Should fail when artifact not available.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldFailWhenArtifactNotAvailable() throws Exception {
    artifactCollectEventListener.onMessage(
        aCollectEvent()
            .withArtifact(
                anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactSourceId(ARTIFACT_SOURCE_ID).build())
            .build());

    verify(collectorServiceMap).get(anyString());
    verify(artifactCollectorService).collect(ARTIFACT_SOURCE, Collections.emptyMap());
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.RUNNING);
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.FAILED);
  }

  /**
   * Should collect and update artifact.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldCollectAndUpdateArtifact() throws Exception {
    when(artifactCollectorService.collect(ARTIFACT_SOURCE, Collections.emptyMap()))
        .thenReturn(Collections.singletonList(ARTIFACT_FILE));

    artifactCollectEventListener.onMessage(
        aCollectEvent()
            .withArtifact(
                anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactSourceId(ARTIFACT_SOURCE_ID).build())
            .build());

    verify(collectorServiceMap).get(anyString());
    verify(artifactCollectorService).collect(ARTIFACT_SOURCE, Collections.emptyMap());

    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.RUNNING);
    verify(artifactService).addArtifactFile(ARTIFACT_ID, APP_ID, Collections.singletonList(ARTIFACT_FILE));
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.READY);
    verify(notificationService).sendNotificationAsync(any(ApprovalNotification.class));
  }

  /**
   * Should fail to collect artifact when source is missing.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldFailToCollectArtifactWhenSourceIsMissing() throws Exception {
    when(artifactCollectorService.collect(ARTIFACT_SOURCE, Collections.emptyMap()))
        .thenReturn(Collections.singletonList(ARTIFACT_FILE));

    artifactCollectEventListener.onMessage(
        aCollectEvent().withArtifact(anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).build()).build());

    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.RUNNING);
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.FAILED);
  }
}
