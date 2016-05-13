package software.wings.collect;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.Artifact.Builder.anArtifact;
import static software.wings.beans.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.ArtifactSourceMetadata.Builder.anArtifactSourceMetadata;
import static software.wings.beans.JenkinsArtifactSource.Builder.aJenkinsArtifactSource;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;
import static software.wings.collect.CollectEvent.Builder.aCollectEvent;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Key;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Artifact.Status;
import software.wings.beans.ArtifactFile;
import software.wings.beans.ArtifactSource;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.ArtifactService;

import java.util.Collections;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
public class ArtifactCollectEventListenerTest extends WingsBaseTest {
  public static final String ARTIFACT_ID = "ARTIFACT_ID";
  public static final String APP_ID = "APP_ID";
  public static final ArtifactFile ARTIFACT_FILE =
      anArtifactFile().withAppId(APP_ID).withUuid("ARTIFACT_FILE_ID").build();
  public static final String RELEASE_ID = "RELEASE_ID";
  public static final String ARTIFACT_SOURCE_NAME = "job1";
  private final ArtifactSource ARTIFACT_SOURCE = aJenkinsArtifactSource().withSourceName(ARTIFACT_SOURCE_NAME).build();

  @InjectMocks @Inject private ArtifactCollectEventListener artifactCollectEventListener;

  @Mock private ArtifactService artifactService;

  @Mock private Injector injector;

  @Mock private ArtifactCollectorService artifactCollectorService;

  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(artifactService, injector, artifactCollectorService);
    }
  };

  @Before
  public void setupMocks() {
    when(injector.getInstance(any(Key.class))).thenReturn(artifactCollectorService);
  }

  @Test
  public void shouldFailWhenArtifactNotAvailable() throws Exception {
    artifactCollectEventListener.onMessage(
        aCollectEvent()
            .withArtifact(anArtifact()
                              .withUuid(ARTIFACT_ID)
                              .withAppId(APP_ID)
                              .withRelease(aRelease()
                                               .withUuid(RELEASE_ID)
                                               .withArtifactSources(Lists.newArrayList(ARTIFACT_SOURCE))
                                               .build())
                              .withArtifactSourceMetadatas(Collections.singletonList(
                                  anArtifactSourceMetadata().withArtifactSourceName(ARTIFACT_SOURCE_NAME).build()))
                              .build())
            .build());

    verify(injector).getInstance(any(Key.class));
    verify(artifactCollectorService).collect(ARTIFACT_SOURCE, Collections.emptyMap());
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.RUNNING);
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.FAILED);
  }

  @Test
  public void shouldCollectAndUpdateArtifact() throws Exception {
    when(artifactCollectorService.collect(ARTIFACT_SOURCE, Collections.emptyMap()))
        .thenReturn(Collections.singletonList(ARTIFACT_FILE));

    artifactCollectEventListener.onMessage(
        aCollectEvent()
            .withArtifact(anArtifact()
                              .withUuid(ARTIFACT_ID)
                              .withAppId(APP_ID)
                              .withRelease(aRelease()
                                               .withUuid(RELEASE_ID)
                                               .withArtifactSources(Lists.newArrayList(ARTIFACT_SOURCE))
                                               .build())
                              .withArtifactSourceMetadatas(Collections.singletonList(
                                  anArtifactSourceMetadata().withArtifactSourceName(ARTIFACT_SOURCE_NAME).build()))
                              .build())
            .build());

    verify(injector).getInstance(any(Key.class));
    verify(artifactCollectorService).collect(ARTIFACT_SOURCE, Collections.emptyMap());

    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.RUNNING);
    verify(artifactService).addArtifactFile(ARTIFACT_ID, APP_ID, Collections.singletonList(ARTIFACT_FILE));
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.READY);
  }

  @Test
  public void shouldFailToCollectArtifactWhenSourceIsMissing() throws Exception {
    when(artifactCollectorService.collect(ARTIFACT_SOURCE, Collections.emptyMap()))
        .thenReturn(Collections.singletonList(ARTIFACT_FILE));

    artifactCollectEventListener.onMessage(
        aCollectEvent()
            .withArtifact(
                anArtifact()
                    .withUuid(ARTIFACT_ID)
                    .withAppId(APP_ID)
                    .withRelease(aRelease().withUuid(RELEASE_ID).withArtifactSources(Lists.newArrayList()).build())
                    .withArtifactSourceMetadatas(Collections.singletonList(
                        anArtifactSourceMetadata().withArtifactSourceName(ARTIFACT_SOURCE_NAME).build()))
                    .build())
            .build());

    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.RUNNING);
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.FAILED);
  }
}
