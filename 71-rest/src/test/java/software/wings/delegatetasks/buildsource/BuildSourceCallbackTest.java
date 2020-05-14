package software.wings.delegatetasks.buildsource;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.VGLIJIN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Maps.newHashMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.Owner;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class BuildSourceCallbackTest extends WingsBaseTest {
  private static final String ARTIFACT_STREAM_ID_1 = "ARTIFACT_STREAM_ID_1";
  private static final String ARTIFACT_STREAM_ID_2 = "ARTIFACT_STREAM_ID_2";

  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactCollectionUtils artifactCollectionUtils;
  @Mock private ArtifactService artifactService;
  @Mock private TriggerService triggerService;
  @Mock private DeploymentTriggerService deploymentTriggerService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ExecutorService executorService;

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

  private static final Artifact ARTIFACT_1 = anArtifact().withMetadata(newHashMap("buildNo", "1")).build();
  private static final Artifact ARTIFACT_2 = anArtifact().withMetadata(newHashMap("buildNo", "2")).build();

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
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccess() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    buildSourceCallback.handleResponseForSuccessInternal(prepareBuildSourceExecutionResponse(true), ARTIFACT_STREAM);
    verify(artifactStreamService, never()).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_1, STABLE.name());
    verify(triggerService)
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_1, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccessTriggerRefactor() {
    when(featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, ACCOUNT_ID)).thenReturn(true);
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    buildSourceCallback.handleResponseForSuccessInternal(prepareBuildSourceExecutionResponse(true), ARTIFACT_STREAM);
    verify(artifactStreamService, never()).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_1, STABLE.name());
    verify(deploymentTriggerService)
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_1, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldUpdateCollectionStatus() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_2);
    buildSourceCallback.handleResponseForSuccessInternal(
        prepareBuildSourceExecutionResponse(true), ARTIFACT_STREAM_UNSTABLE);

    verify(artifactStreamService).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_2, STABLE.name());
    verify(triggerService, never())
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_2, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotUpdateCollectionStatus() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_2);
    buildSourceCallback.handleResponseForSuccessInternal(
        prepareBuildSourceExecutionResponse(false), ARTIFACT_STREAM_UNSTABLE);
    verify(artifactStreamService, never()).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_2, STABLE.name());
    verify(triggerService, never())
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_2, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldHandleNullBuildSourceResponse() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_2);
    buildSourceCallback.handleResponseForSuccessInternal(
        BuildSourceExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build(),
        ARTIFACT_STREAM_UNSTABLE);
    verify(artifactStreamService, never()).updateCollectionStatus(ACCOUNT_ID, ARTIFACT_STREAM_ID_2, STABLE.name());
    verify(triggerService, never())
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_2, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNotify() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    BuildSourceExecutionResponse buildSourceExecutionResponse = prepareBuildSourceExecutionResponse(true);
    buildSourceExecutionResponse.getBuildSourceResponse().setBuildDetails(null);

    buildSourceCallback.notify(newHashMap("", buildSourceExecutionResponse));
    verify(executorService, times(1)).submit(any(Runnable.class));
    verify(artifactStreamService, times(1)).get(any());
    verify(artifactStreamService, never()).updateCollectionStatus(any(), any(), any());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNotifyOnFailedResponse() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    BuildSourceExecutionResponse buildSourceExecutionResponse = prepareBuildSourceExecutionResponse(true);
    buildSourceExecutionResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    buildSourceExecutionResponse.getBuildSourceResponse().setBuildDetails(null);

    buildSourceCallback.notify(newHashMap("", buildSourceExecutionResponse));
    verify(executorService, never()).submit(any(Runnable.class));
    verify(artifactStreamService, times(1)).get(any());
    verify(artifactStreamService, times(1)).updateFailedCronAttempts(any(), any(), anyInt());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNotifyOnErrorNotifyResponseDataResponse() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    buildSourceCallback.notify(newHashMap("", ErrorNotifyResponseData.builder().build()));
    verify(executorService, never()).submit(any(Runnable.class));
    verify(artifactStreamService, times(2)).get(any());
    verify(artifactStreamService, times(1)).updateFailedCronAttempts(any(), any(), anyInt());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNotifyWithExecutorRejectedQueueException() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    when(executorService.submit(any(Runnable.class))).thenThrow(RejectedExecutionException.class);
    BuildSourceExecutionResponse buildSourceExecutionResponse = prepareBuildSourceExecutionResponse(true);
    buildSourceExecutionResponse.getBuildSourceResponse().setBuildDetails(null);

    buildSourceCallback.notify(newHashMap("", prepareBuildSourceExecutionResponse(true)));
    verify(executorService, times(1)).submit(any(Runnable.class));
    verify(artifactStreamService, times(1)).get(any());
    verify(artifactStreamService, never()).updateCollectionStatus(any(), any(), any());
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void testNotifyThrowsArtifactStreamNotFound() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(null);
    assertThatThrownBy(() -> buildSourceCallback.notify(newHashMap("", prepareBuildSourceExecutionResponse(true))))
        .isInstanceOf(ArtifactStreamNotFound.class);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void testNotifyErrorThrowsArtifactStreamNotFound() {
    buildSourceCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(null);
    assertThatThrownBy(() -> buildSourceCallback.notifyError(newHashMap("", prepareBuildSourceExecutionResponse(true))))
        .isInstanceOf(ArtifactStreamNotFound.class);
  }

  private BuildSourceExecutionResponse prepareBuildSourceExecutionResponse(boolean stable) {
    return BuildSourceExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .buildSourceResponse(
            BuildSourceResponse.builder().buildDetails(asList(BUILD_DETAILS_1, BUILD_DETAILS_2)).stable(stable).build())
        .build();
  }
}
