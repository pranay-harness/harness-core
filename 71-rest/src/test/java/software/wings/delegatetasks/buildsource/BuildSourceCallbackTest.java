package software.wings.delegatetasks.buildsource;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamCollectionStatus.STABLE;
import static software.wings.beans.artifact.ArtifactStreamCollectionStatus.UNSTABLE;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

public class BuildSourceCallbackTest extends WingsBaseTest {
  private static final String ARTIFACT_STREAM_ID_1 = "ARTIFACT_STREAM_ID_1";
  private static final String ARTIFACT_STREAM_ID_2 = "ARTIFACT_STREAM_ID_2";

  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactCollectionUtils artifactCollectionUtils;
  @Mock private ArtifactService artifactService;
  @Mock private TriggerService triggerService;
  @Mock private DeploymentTriggerService deploymentTriggerService;
  @Mock private FeatureFlagService featureFlagService;

  @InjectMocks @Inject private BuildSourceCallback buildSourceCallback;

  private final ArtifactStream ARTIFACT_STREAM = DockerArtifactStream.builder()
                                                     .uuid(ARTIFACT_STREAM_ID_1)
                                                     .sourceName(ARTIFACT_STREAM_NAME)
                                                     .appId(APP_ID)
                                                     .settingId(SETTING_ID)
                                                     .serviceId(SERVICE_ID)
                                                     .imageName("image_name")
                                                     .build();

  private final ArtifactStream ARTIFACT_STREAM_UNSTABLE = DockerArtifactStream.builder()
                                                              .uuid(ARTIFACT_STREAM_ID_2)
                                                              .sourceName(ARTIFACT_STREAM_NAME)
                                                              .appId(APP_ID)
                                                              .settingId(SETTING_ID)
                                                              .serviceId(SERVICE_ID)
                                                              .imageName("image_name")
                                                              .build();

  private static final BuildDetails BUILD_DETAILS_1 = aBuildDetails().withNumber("1").build();
  private static final BuildDetails BUILD_DETAILS_2 = aBuildDetails().withNumber("2").build();

  private static final Artifact ARTIFACT_1 = anArtifact().withMetadata(Maps.newHashMap("buildNo", "1")).build();
  private static final Artifact ARTIFACT_2 = anArtifact().withMetadata(Maps.newHashMap("buildNo", "2")).build();

  @Before
  public void setupMocks() {
    ARTIFACT_STREAM_UNSTABLE.setCollectionStatus(UNSTABLE.name());
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(ARTIFACT_STREAM);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_2)).thenReturn(ARTIFACT_STREAM_UNSTABLE);
    when(artifactCollectionUtils.getArtifact(ARTIFACT_STREAM, BUILD_DETAILS_1)).thenReturn(ARTIFACT_1);
    when(artifactCollectionUtils.getArtifact(ARTIFACT_STREAM, BUILD_DETAILS_2)).thenReturn(ARTIFACT_2);
    when(artifactCollectionUtils.getArtifact(ARTIFACT_STREAM_UNSTABLE, BUILD_DETAILS_1)).thenReturn(ARTIFACT_1);
    when(artifactCollectionUtils.getArtifact(ARTIFACT_STREAM_UNSTABLE, BUILD_DETAILS_2)).thenReturn(ARTIFACT_2);
    when(artifactService.create(ARTIFACT_1)).thenReturn(ARTIFACT_1);
    when(artifactService.create(ARTIFACT_2)).thenReturn(ARTIFACT_2);
    when(featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, ACCOUNT_ID)).thenReturn(false);
    buildSourceCallback.setAccountId(ACCOUNT_ID);
    buildSourceCallback.setSettingId(SETTING_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccess() {
    shouldNotifyOnSuccessHelper();
    verify(triggerService)
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_1, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccessTriggerRefactor() {
    when(featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, ACCOUNT_ID)).thenReturn(true);
    shouldNotifyOnSuccessHelper();
    verify(deploymentTriggerService)
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_1, asList(ARTIFACT_1, ARTIFACT_2));
  }

  private void shouldNotifyOnSuccessHelper() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    buildSourceCallback.notify(Maps.newHashMap("", prepareBuildSourceExecutionResponse(true)));
    verify(artifactStreamService, never()).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_1, STABLE.name());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateCollectionStatus() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_2);
    buildSourceCallback.notify(Maps.newHashMap("", prepareBuildSourceExecutionResponse(true)));
    verify(artifactStreamService).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_2, STABLE.name());
    verify(triggerService, never())
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_2, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotUpdateCollectionStatus() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_2);
    buildSourceCallback.notify(Maps.newHashMap("", prepareBuildSourceExecutionResponse(false)));
    verify(artifactStreamService, never()).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_2, STABLE.name());
    verify(triggerService, never())
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_2, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleNullBuildSourceResponse() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_2);
    buildSourceCallback.notify(Maps.newHashMap(
        "", BuildSourceExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build()));
    verify(artifactStreamService, never()).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_2, STABLE.name());
    verify(triggerService, never())
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_2, asList(ARTIFACT_1, ARTIFACT_2));
  }

  private BuildSourceExecutionResponse prepareBuildSourceExecutionResponse(boolean stable) {
    return BuildSourceExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .buildSourceResponse(
            BuildSourceResponse.builder().buildDetails(asList(BUILD_DETAILS_1, BUILD_DETAILS_2)).stable(stable).build())
        .build();
  }
}
