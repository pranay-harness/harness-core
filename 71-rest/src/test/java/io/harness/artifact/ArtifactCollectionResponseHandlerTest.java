package io.harness.artifact;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamCollectionStatus.UNSTABLE;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.TriggerService;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class ArtifactCollectionResponseHandlerTest extends CategoryTest {
  @Mock private ExecutorService executorService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private TriggerService triggerService;
  @Mock private ArtifactCollectionUtils artifactCollectionUtils;
  @Mock private FeatureFlagService featureFlagService;

  @Inject @InjectMocks private ArtifactCollectionResponseHandler artifactCollectionResponseHandler;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  private static final BuildDetails BUILD_DETAILS_1 = aBuildDetails().withNumber("1").build();
  private static final BuildDetails BUILD_DETAILS_2 = aBuildDetails().withNumber("2").build();
  private final ArtifactStream ARTIFACT_STREAM = DockerArtifactStream.builder()
                                                     .uuid(ARTIFACT_STREAM_ID)
                                                     .sourceName(ARTIFACT_STREAM_NAME)
                                                     .appId(APP_ID)
                                                     .settingId(SETTING_ID)
                                                     .serviceId(SERVICE_ID)
                                                     .imageName("image_name")
                                                     .accountId(ACCOUNT_ID)
                                                     .build();

  private final String ARTIFACT_STREAM_ID_2 = "ARTIFACT_STREAM_ID_2";
  private final ArtifactStream ARTIFACT_STREAM_UNSTABLE = DockerArtifactStream.builder()
                                                              .uuid(ARTIFACT_STREAM_ID_2)
                                                              .sourceName(ARTIFACT_STREAM_NAME)
                                                              .appId(APP_ID)
                                                              .settingId(SETTING_ID)
                                                              .serviceId(SERVICE_ID)
                                                              .imageName("image_name")
                                                              .accountId(ACCOUNT_ID)
                                                              .build();

  private static final Artifact ARTIFACT_1 = anArtifact().withMetadata(Maps.newHashMap("buildNo", "1")).build();
  private static final Artifact ARTIFACT_2 = anArtifact().withMetadata(Maps.newHashMap("buildNo", "2")).build();

  @Before
  public void setup() {
    ARTIFACT_STREAM_UNSTABLE.setCollectionStatus(UNSTABLE.name());
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(ARTIFACT_STREAM);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_2)).thenReturn(ARTIFACT_STREAM_UNSTABLE);
    when(featureFlagService.isEnabled(eq(FeatureName.ARTIFACT_STREAM_REFACTOR), any())).thenReturn(false);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldHandleInvalidArtifactStream() {
    BuildSourceExecutionResponse buildSourceExecutionResponse = constructBuildSourceExecutionResponse(false, true);
    buildSourceExecutionResponse.setArtifactStreamId("random");
    artifactCollectionResponseHandler.processArtifactCollectionResult(buildSourceExecutionResponse);
    verify(executorService, never()).submit(any(Runnable.class));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldHandleExecutorRejectedQueueException() {
    BuildSourceExecutionResponse buildSourceExecutionResponse = constructBuildSourceExecutionResponse(false, true);
    when(executorService.submit(any(Runnable.class))).thenThrow(RejectedExecutionException.class);
    artifactCollectionResponseHandler.processArtifactCollectionResult(buildSourceExecutionResponse);
    verify(executorService, times(1)).submit(any(Runnable.class));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotProcessHandleFailedResponse() {
    BuildSourceExecutionResponse buildSourceExecutionResponse =
        BuildSourceExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    artifactCollectionResponseHandler.processArtifactCollectionResult(buildSourceExecutionResponse);
    verify(executorService, never()).submit(any(Runnable.class));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldExecuteTrigger() {
    BuildSourceExecutionResponse buildSourceExecutionResponse = constructBuildSourceExecutionResponse(false, true);

    when(artifactCollectionUtils.processBuilds(ARTIFACT_STREAM_UNSTABLE, asList(BUILD_DETAILS_1, BUILD_DETAILS_2)))
        .thenReturn(asList(ARTIFACT_1, ARTIFACT_2));

    artifactCollectionResponseHandler.handleResponseInternal(ARTIFACT_STREAM_UNSTABLE, buildSourceExecutionResponse);

    verify(artifactCollectionUtils).processBuilds(ARTIFACT_STREAM_UNSTABLE, asList(BUILD_DETAILS_1, BUILD_DETAILS_2));
    verify(triggerService)
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_2, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotExecuteTrigger() {
    BuildSourceExecutionResponse buildSourceExecutionResponse = constructBuildSourceExecutionResponse(false, false);

    when(artifactCollectionUtils.processBuilds(ARTIFACT_STREAM_UNSTABLE, asList(BUILD_DETAILS_1, BUILD_DETAILS_2)))
        .thenReturn(asList(ARTIFACT_1, ARTIFACT_2));

    artifactCollectionResponseHandler.handleResponseInternal(ARTIFACT_STREAM_UNSTABLE, buildSourceExecutionResponse);

    verify(artifactCollectionUtils).processBuilds(ARTIFACT_STREAM_UNSTABLE, asList(BUILD_DETAILS_1, BUILD_DETAILS_2));
    verify(triggerService, never())
        .triggerExecutionPostArtifactCollectionAsync(
            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID_2, asList(ARTIFACT_1, ARTIFACT_2));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldHandleCleanup() {
    BuildSourceExecutionResponse buildSourceExecutionResponse = constructBuildSourceExecutionResponse(true, false);
    when(artifactService.deleteArtifactsByUniqueKey(any(), any(), any())).thenReturn(true);
    artifactCollectionResponseHandler.handleResponseInternal(ARTIFACT_STREAM_UNSTABLE, buildSourceExecutionResponse);
    verify(artifactService).deleteArtifactsByUniqueKey(eq(ARTIFACT_STREAM_UNSTABLE), any(), any());
  }

  private BuildSourceExecutionResponse constructBuildSourceExecutionResponse(boolean cleanup, boolean stable) {
    BuildSourceResponse buildSourceResponse;
    if (cleanup) {
      buildSourceResponse =
          BuildSourceResponse.builder()
              .toBeDeletedKeys(new HashSet<>(asList(BUILD_DETAILS_1.getNumber(), BUILD_DETAILS_2.getNumber())))
              .cleanup(true)
              .build();
    } else {
      buildSourceResponse =
          BuildSourceResponse.builder().buildDetails(asList(BUILD_DETAILS_1, BUILD_DETAILS_2)).stable(stable).build();
    }

    return BuildSourceExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .artifactStreamId(ARTIFACT_STREAM_ID_2)
        .buildSourceResponse(buildSourceResponse)
        .build();
  }
}