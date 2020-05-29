package software.wings.service.impl.artifact;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.artifact.ArtifactCollectionPTaskClientParams;
import io.harness.artifact.ArtifactCollectionPTaskServiceClient;
import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;

public class ArtifactStreamPTaskHelperTest extends CategoryTest {
  private static final String PERPETUAL_TASK_ID = "PERPETUAL_TASK_ID";

  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private PerpetualTaskServiceClientRegistry clientRegistry;
  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock private ArtifactCollectionPTaskServiceClient artifactCollectionPTaskServiceClient;

  @Inject @InjectMocks private ArtifactStreamPTaskHelper artifactStreamPTaskHelper;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    when(clientRegistry.getClient(eq(PerpetualTaskType.ARTIFACT_COLLECTION)))
        .thenReturn(artifactCollectionPTaskServiceClient);
    when(artifactCollectionPTaskServiceClient.create(eq(ACCOUNT_ID), any(ArtifactCollectionPTaskClientParams.class)))
        .thenReturn(PERPETUAL_TASK_ID);
  }

  @Test
  @Owner(developers = OwnerRule.GARVIT)
  @Category(UnitTests.class)
  public void testCreatePerpetualTask() {
    ArtifactStream artifactStreamWithoutId = prepareArtifactStreamWithoutId();
    assertThatThrownBy(() -> artifactStreamPTaskHelper.createPerpetualTask(artifactStreamWithoutId))
        .isInstanceOf(GeneralException.class);

    ArtifactStream artifactStream = prepareArtifactStream();
    when(artifactStreamService.attachPerpetualTaskId(eq(artifactStream), eq(PERPETUAL_TASK_ID))).thenReturn(true);
    artifactStreamPTaskHelper.createPerpetualTask(artifactStream);
    verify(artifactCollectionPTaskServiceClient, times(1))
        .create(eq(ACCOUNT_ID), any(ArtifactCollectionPTaskClientParams.class));
    verify(perpetualTaskService, never()).deleteTask(eq(ACCOUNT_ID), eq(PERPETUAL_TASK_ID));

    when(artifactStreamService.attachPerpetualTaskId(eq(artifactStream), eq(PERPETUAL_TASK_ID))).thenReturn(false);
    artifactStreamPTaskHelper.createPerpetualTask(artifactStream);
    verify(artifactCollectionPTaskServiceClient, times(2))
        .create(eq(ACCOUNT_ID), any(ArtifactCollectionPTaskClientParams.class));
    verify(perpetualTaskService, times(1)).deleteTask(eq(ACCOUNT_ID), eq(PERPETUAL_TASK_ID));

    when(artifactStreamService.attachPerpetualTaskId(eq(artifactStream), eq(PERPETUAL_TASK_ID)))
        .thenThrow(new RuntimeException());
    artifactStreamPTaskHelper.createPerpetualTask(artifactStream);
    verify(artifactCollectionPTaskServiceClient, times(3))
        .create(eq(ACCOUNT_ID), any(ArtifactCollectionPTaskClientParams.class));
    verify(perpetualTaskService, times(1)).deleteTask(eq(ACCOUNT_ID), eq(PERPETUAL_TASK_ID));

    ArtifactStream artifactStreamWithPerpetualTaskId = prepareArtifactStream();
    artifactStreamWithPerpetualTaskId.setPerpetualTaskId(PERPETUAL_TASK_ID);
    assertThatThrownBy(() -> artifactStreamPTaskHelper.createPerpetualTask(artifactStreamWithPerpetualTaskId))
        .isInstanceOf(InvalidRequestException.class);
  }

  private ArtifactStream prepareArtifactStream() {
    return DockerArtifactStream.builder()
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .uuid(ARTIFACT_STREAM_ID)
        .settingId(SETTING_ID)
        .imageName("wingsplugins/todolist")
        .autoPopulate(true)
        .serviceId(SERVICE_ID)
        .build();
  }

  private ArtifactStream prepareArtifactStreamWithoutId() {
    return DockerArtifactStream.builder()
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .settingId(SETTING_ID)
        .imageName("wingsplugins/todolist")
        .serviceId(SERVICE_ID)
        .build();
  }
}
