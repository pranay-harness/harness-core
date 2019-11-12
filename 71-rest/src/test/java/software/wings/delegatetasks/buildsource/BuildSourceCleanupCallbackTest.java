package software.wings.delegatetasks.buildsource;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamCollectionStatus.UNSTABLE;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class BuildSourceCleanupCallbackTest extends WingsBaseTest {
  private static final String ARTIFACT_STREAM_ID_1 = "ARTIFACT_STREAM_ID_1";
  private static final String ARTIFACT_STREAM_ID_2 = "ARTIFACT_STREAM_ID_2";
  private static final String ARTIFACT_STREAM_ID_3 = "ARTIFACT_STREAM_ID_3";

  @Mock private ArtifactCollectionUtils artifactCollectionUtils;
  @Mock private ArtifactService artifactService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private Query query;
  @Mock private MorphiaIterator<Artifact, Artifact> artifactIterator;
  @Mock private ExecutorService executorService;

  @InjectMocks @Inject private BuildSourceCleanupCallback buildSourceCleanupCallback;

  private final ArtifactStream ARTIFACT_STREAM_UNSTABLE = DockerArtifactStream.builder()
                                                              .uuid(ARTIFACT_STREAM_ID_2)
                                                              .sourceName(ARTIFACT_STREAM_NAME)
                                                              .appId(APP_ID)
                                                              .settingId(SETTING_ID)
                                                              .serviceId(SERVICE_ID)
                                                              .imageName("image_name")
                                                              .build();

  private final ArtifactStream ARTIFACT_STREAM = DockerArtifactStream.builder()
                                                     .uuid(ARTIFACT_STREAM_ID_1)
                                                     .sourceName(ARTIFACT_STREAM_NAME)
                                                     .appId(APP_ID)
                                                     .settingId(SETTING_ID)
                                                     .serviceId(SERVICE_ID)
                                                     .imageName("image_name")
                                                     .build();

  AmiArtifactStream amiArtifactStream = AmiArtifactStream.builder()
                                            .accountId(ACCOUNT_ID)
                                            .uuid(ARTIFACT_STREAM_ID_3)
                                            .appId(APP_ID)
                                            .settingId(SETTING_ID)
                                            .region("us-east-1")
                                            .autoPopulate(true)
                                            .serviceId(SERVICE_ID)
                                            .build();

  Artifact artifact = Artifact.Builder.anArtifact()
                          .withUuid(ARTIFACT_ID)
                          .withArtifactStreamId(ARTIFACT_STREAM_ID)
                          .withAppId(APP_ID)
                          .withSettingId(SETTING_ID)
                          .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                          .withRevision("1.0")
                          .build();

  private static final Artifact ARTIFACT_1 = anArtifact().withMetadata(Maps.newHashMap("buildNo", "1")).build();
  private static final Artifact ARTIFACT_2 = anArtifact().withMetadata(Maps.newHashMap("buildNo", "2")).build();

  private static final BuildDetails BUILD_DETAILS_1 = aBuildDetails().withNumber("1").build();
  private static final BuildDetails BUILD_DETAILS_2 = aBuildDetails().withNumber("2").build();

  @Before
  public void setupMocks() {
    ARTIFACT_STREAM_UNSTABLE.setCollectionStatus(UNSTABLE.name());
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(ARTIFACT_STREAM);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_1)).thenReturn(ARTIFACT_STREAM);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID_3)).thenReturn(amiArtifactStream);
    when(artifactCollectionUtils.getArtifact(ARTIFACT_STREAM_UNSTABLE, BUILD_DETAILS_1)).thenReturn(ARTIFACT_1);
    when(artifactCollectionUtils.getArtifact(ARTIFACT_STREAM_UNSTABLE, BUILD_DETAILS_2)).thenReturn(ARTIFACT_2);
    when(artifactCollectionUtils.getArtifact(ARTIFACT_STREAM, BUILD_DETAILS_1)).thenReturn(ARTIFACT_1);
    when(artifactCollectionUtils.getArtifact(ARTIFACT_STREAM, BUILD_DETAILS_2)).thenReturn(ARTIFACT_2);

    when(artifactService.create(ARTIFACT_1)).thenReturn(ARTIFACT_1);
    when(artifactService.create(ARTIFACT_2)).thenReturn(ARTIFACT_2);
    when(featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, ACCOUNT_ID)).thenReturn(false);
    buildSourceCleanupCallback.setAccountId(ACCOUNT_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccessWithEmptyBuilds() {
    buildSourceCleanupCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);

    BuildSourceExecutionResponse buildSourceExecutionResponse = prepareBuildSourceExecutionResponse(true);
    buildSourceExecutionResponse.getBuildSourceResponse().setBuildDetails(emptyList());
    buildSourceCleanupCallback.handleResponseForSuccessInternal(buildSourceExecutionResponse, ARTIFACT_STREAM);
    verify(artifactService, never()).prepareArtifactWithMetadataQuery(any());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccessWithDeleteArtifacts() {
    buildSourceCleanupCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    when(artifactService.prepareArtifactWithMetadataQuery(any())).thenReturn(query);
    when(query.fetch()).thenReturn(artifactIterator);

    when(artifactIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(artifactIterator.next()).thenReturn(anArtifact().build());

    buildSourceCleanupCallback.handleResponseForSuccessInternal(
        prepareBuildSourceExecutionResponse(true), ARTIFACT_STREAM);
    verify(artifactService, times(1)).deleteArtifacts(any());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccessWithAMIDeleteArtifacts() {
    buildSourceCleanupCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_3);
    when(artifactService.prepareArtifactWithMetadataQuery(any())).thenReturn(query);
    when(query.fetch()).thenReturn(artifactIterator);

    when(artifactIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(artifactIterator.next()).thenReturn(artifact);

    buildSourceCleanupCallback.handleResponseForSuccessInternal(
        prepareBuildSourceExecutionResponse(true), amiArtifactStream);
    verify(artifactService, times(1)).deleteArtifacts(any());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSkipDeleteWithEmptyArtifacts() {
    buildSourceCleanupCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_3);
    when(artifactService.prepareArtifactWithMetadataQuery(any())).thenReturn(query);
    when(query.fetch()).thenReturn(artifactIterator);

    when(artifactIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(artifactIterator.next()).thenReturn(null);

    buildSourceCleanupCallback.handleResponseForSuccessInternal(
        prepareBuildSourceExecutionResponse(true), amiArtifactStream);
    verify(artifactService, times(0)).deleteArtifacts(any());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccess() {
    buildSourceCleanupCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    when(artifactService.prepareArtifactWithMetadataQuery(any())).thenReturn(query);
    when(query.fetch()).thenReturn(artifactIterator);

    when(artifactIterator.hasNext()).thenReturn(true).thenReturn(false);
    Map<String, String> metadataMap = new HashMap<>();
    metadataMap.put(ArtifactMetadataKeys.buildNo, "1");
    when(artifactIterator.next()).thenReturn(anArtifact().withMetadata(metadataMap).build());

    buildSourceCleanupCallback.handleResponseForSuccessInternal(
        prepareBuildSourceExecutionResponse(true), ARTIFACT_STREAM);
    verify(artifactService, never()).deleteArtifacts(any());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotifyOnSuccessWithNullResponse() {
    buildSourceCleanupCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    BuildSourceExecutionResponse buildSourceExecutionResponse = prepareBuildSourceExecutionResponse(true);
    buildSourceExecutionResponse.setBuildSourceResponse(null);
    buildSourceCleanupCallback.handleResponseForSuccessInternal(buildSourceExecutionResponse, ARTIFACT_STREAM);
    verify(artifactService, never()).deleteArtifacts(any());
  }

  @Test
  @Category(UnitTests.class)
  public void testNotifyWithExecutorRejectedQueueException() {
    buildSourceCleanupCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    when(executorService.submit(any(Runnable.class))).thenThrow(RejectedExecutionException.class);
    BuildSourceExecutionResponse buildSourceExecutionResponse = prepareBuildSourceExecutionResponse(true);
    buildSourceExecutionResponse.getBuildSourceResponse().setBuildDetails(null);

    buildSourceCleanupCallback.notify(Maps.newHashMap("", prepareBuildSourceExecutionResponse(true)));
    verify(executorService, times(1)).submit(any(Runnable.class));
  }

  @Test
  @Category(UnitTests.class)
  public void testNotifyOnErrorNotifyResponseDataResponse() {
    buildSourceCleanupCallback.setArtifactStreamId(ARTIFACT_STREAM_ID_1);
    buildSourceCleanupCallback.notify(Maps.newHashMap("", ErrorNotifyResponseData.builder().build()));
    verify(executorService, never()).submit(any(Runnable.class));
  }

  private BuildSourceExecutionResponse prepareBuildSourceExecutionResponse(boolean stable) {
    return BuildSourceExecutionResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .buildSourceResponse(
            BuildSourceResponse.builder().buildDetails(asList(BUILD_DETAILS_1, BUILD_DETAILS_2)).stable(stable).build())
        .build();
  }
}
