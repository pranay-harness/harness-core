package software.wings.service.impl.artifact;

import static io.harness.perpetualtask.PerpetualTaskType.ARTIFACT_COLLECTION;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.artifact.ArtifactCollectionPTaskServiceClient;
import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
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

public class ArtifactStreamPerpetualTaskManagerTest extends CategoryTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private PerpetualTaskServiceClientRegistry clientRegistry;
  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock private ArtifactCollectionPTaskServiceClient artifactCollectionPTaskServiceClient;

  @Inject @InjectMocks ArtifactStreamPerpetualTaskManager manager;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private ArtifactStream artifactStream = DockerArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .uuid(ARTIFACT_STREAM_ID)
                                              .settingId(SETTING_ID)
                                              .imageName("wingsplugins/todolist")
                                              .autoPopulate(true)
                                              .serviceId(SERVICE_ID)
                                              .build();

  @Before
  public void setUp() {
    when(clientRegistry.getClient(eq(PerpetualTaskType.ARTIFACT_COLLECTION)))
        .thenReturn(artifactCollectionPTaskServiceClient);
    when(perpetualTaskService.getPerpetualTaskType(anyString())).thenReturn(ARTIFACT_COLLECTION);
  }

  @Test
  @Owner(developers = OwnerRule.SRINIVAS)
  @Category(UnitTests.class)
  public void shouldCreatePerpetualTask() {
    manager.onSaved(artifactStream);
    verify(artifactStreamService).attachPerpetualTaskId(eq(artifactStream), anyString());
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = OwnerRule.SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotCreatePerpetualTask() {
    ArtifactStream artifactStream = DockerArtifactStream.builder()
                                        .accountId(ACCOUNT_ID)
                                        .appId(APP_ID)
                                        .settingId(SETTING_ID)
                                        .imageName("wingsplugins/todolist")
                                        .serviceId(SERVICE_ID)
                                        .build();
    manager.onSaved(artifactStream);
  }
}