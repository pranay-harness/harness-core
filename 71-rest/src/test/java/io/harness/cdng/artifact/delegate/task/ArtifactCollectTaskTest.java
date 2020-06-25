package io.harness.cdng.artifact.delegate.task;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.DockerArtifactServiceImpl;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.exception.ArtifactServerException;
import io.harness.rule.Owner;
import io.harness.tasks.Cd1SetupFields;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.TaskType;

import java.util.concurrent.TimeUnit;

public class ArtifactCollectTaskTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private DockerArtifactServiceImpl dockerArtifactService;

  private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
  private final DelegateTask collectionTask = DelegateTask.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
                                                  .waitId("123456789")
                                                  .data(TaskData.builder()
                                                            .async(true)
                                                            .taskType(TaskType.ARTIFACT_COLLECT_TASK.name())
                                                            .timeout(DEFAULT_TIMEOUT)
                                                            .build())
                                                  .build();

  @InjectMocks
  @Spy
  private final ArtifactCollectTask artifactCollectTask =
      (ArtifactCollectTask) TaskType.ARTIFACT_COLLECT_TASK.getDelegateRunnableTask(
          DelegateTaskPackage.builder().delegateId("delid1").delegateTask(collectionTask).build(),
          notifyResponseData -> {}, () -> true);

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldCollectLatestDockerPublicImage() {
    ArtifactTaskParameters taskParameters =
        ArtifactTaskParameters.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .attributes(DockerArtifactSourceAttributes.builder()
                            .dockerhubConnector("CONNECTOR")
                            .imagePath("imagePath")
                            .tag("tag")
                            .build())
            .connectorConfig(
                DockerhubConnectorConfig.builder().identifier("CONNECTOR").registryUrl("CONNECTOR_URL").build())
            .build();
    DockerArtifactAttributes dockerArtifactAttributes =
        DockerArtifactAttributes.builder().imagePath("imagePath").tag("tag").dockerHubConnector("CONNECTOR").build();
    doReturn(dockerArtifactService)
        .when(artifactCollectTask)
        .getDelegateArtifactService(any(ArtifactTaskParameters.class));
    doReturn(dockerArtifactAttributes)
        .when(dockerArtifactService)
        .getLastSuccessfulBuild(
            APP_ID, taskParameters.getAttributes(), (DockerhubConnectorConfig) taskParameters.getConnectorConfig());
    ResponseData run = artifactCollectTask.run(taskParameters);
    assertThat(run).isInstanceOf(ArtifactTaskResponse.class);
    ArtifactTaskResponse artifactTaskResponse = (ArtifactTaskResponse) run;
    assertThat(artifactTaskResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(artifactTaskResponse.getArtifactAttributes()).isEqualTo(dockerArtifactAttributes);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldFailCollectingDockerPublicImage() {
    ArtifactTaskParameters taskParameters =
        ArtifactTaskParameters.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .attributes(DockerArtifactSourceAttributes.builder()
                            .dockerhubConnector("CONNECTOR")
                            .imagePath("imagePath")
                            .tag("tag")
                            .build())
            .connectorConfig(
                DockerhubConnectorConfig.builder().identifier("CONNECTOR").registryUrl("CONNECTOR_URL").build())
            .build();
    doReturn(dockerArtifactService)
        .when(artifactCollectTask)
        .getDelegateArtifactService(any(ArtifactTaskParameters.class));
    doThrow(new ArtifactServerException("Mock Exception"))
        .when(dockerArtifactService)
        .getLastSuccessfulBuild(
            any(String.class), any(ArtifactSourceAttributes.class), any(DockerhubConnectorConfig.class));
    ResponseData run = artifactCollectTask.run(taskParameters);
    assertThat(run).isInstanceOf(ArtifactTaskResponse.class);
    ArtifactTaskResponse artifactTaskResponse = (ArtifactTaskResponse) run;
    assertThat(artifactTaskResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }
}