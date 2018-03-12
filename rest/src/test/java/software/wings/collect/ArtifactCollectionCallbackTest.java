package software.wings.collect;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_PATH;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.JOB_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.waitnotify.ListNotifyResponseData.Builder.aListNotifyResponseData;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.EventEmitter;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.NotificationService;

/**
 * Created by rishi on 12/21/16.
 */
public class ArtifactCollectionCallbackTest extends WingsBaseTest {
  @Mock private ArtifactService artifactService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private EventEmitter eventEmitter;
  @Mock private NotificationService notificationService;

  @InjectMocks @Inject private ArtifactCollectionCallback artifactCollectionCallback;

  private final Artifact ARTIFACT =
      anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactStreamId(ARTIFACT_STREAM_ID).build();

  /**
   * The constant ARTIFACT_STREAM_NAME.
   */
  private final ArtifactStream ARTIFACT_SOURCE = aJenkinsArtifactStream()
                                                     .withSourceName(ARTIFACT_STREAM_NAME)
                                                     .withAppId(APP_ID)
                                                     .withSettingId(SETTING_ID)
                                                     .withJobname(JOB_NAME)
                                                     .withServiceId(SERVICE_ID)
                                                     .withArtifactPaths(asList(ARTIFACT_PATH))
                                                     .withAutoApproveForProduction(false)
                                                     .build();

  /**
   * The constant ARTIFACT_FILE.
   */
  public static final ArtifactFile ARTIFACT_FILE =
      anArtifactFile().withAppId(APP_ID).withUuid("ARTIFACT_FILE_ID").build();

  /**
   * Setup mocks.
   */
  @Before
  public void setupMocks() {
    when(artifactService.get(APP_ID, ARTIFACT_ID)).thenReturn(ARTIFACT);
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(ARTIFACT_SOURCE);
    artifactCollectionCallback.setAppId(APP_ID);
    artifactCollectionCallback.setArtifactId(ARTIFACT_ID);
  }

  /**
   * Should shouldSendTask
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldNotify() {
    artifactCollectionCallback.notify(Maps.newHashMap("", aListNotifyResponseData().addData(ARTIFACT_FILE).build()));
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, Status.APPROVED, DOWNLOADED);
    verify(artifactService).addArtifactFile(ARTIFACT_ID, APP_ID, Lists.newArrayList(ARTIFACT_FILE));
  }
}
