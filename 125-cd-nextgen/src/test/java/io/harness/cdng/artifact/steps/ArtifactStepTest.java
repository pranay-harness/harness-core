package io.harness.cdng.artifact.steps;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.utils.ArtifactStepHelper;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.DelegateTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ArtifactStepTest extends CDNGBaseTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock ArtifactStepHelper artifactStepHelper;
  @Inject KryoSerializer kryoSerializer;
  @Spy @InjectMocks ArtifactStep artifactStep;

  Map<String, ResponseData> responseDataMap = new HashMap<>();

  @Before
  public void beforeClass() {
    responseDataMap = new HashMap<>();
    Reflect.on(artifactStep).set("kryoSerializer", kryoSerializer);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testObtainingArtifactTaskForDocker() {
    Ambiance ambiance =
        Ambiance.newBuilder()
            .putAllSetupAbstractions(ImmutableMap.of("accountId", "ACCOUNT_ID", "expressionFunctorToken", "1234"))
            .build();
    ArtifactStepParameters stepParameters = getStepParametersForDocker();
    when(artifactStepHelper.toSourceDelegateRequest(artifactStep.applyArtifactsOverlay(stepParameters), ambiance))
        .thenReturn(getDelegateRequest());
    when(artifactStepHelper.getArtifactStepTaskType(artifactStep.applyArtifactsOverlay(stepParameters)))
        .thenReturn(TaskType.DOCKER_ARTIFACT_TASK_NG.name());
    TaskRequest taskRequest = artifactStep.getTaskRequest(ambiance, stepParameters);
    assertThat(taskRequest.getDelegateTaskRequest()).isNotNull();

    DelegateTaskRequest delegateTaskRequest = taskRequest.getDelegateTaskRequest();
    assertThat(delegateTaskRequest.getAccountId()).isEqualTo("ACCOUNT_ID");
    assertThat(delegateTaskRequest.getDetails().getType().getType()).isEqualTo(TaskType.DOCKER_ARTIFACT_TASK_NG.name());

    TaskParameters taskParameters = (TaskParameters) kryoSerializer.asInflatedObject(
        delegateTaskRequest.getDetails().getKryoParameters().toByteArray());
    assertThat(taskParameters).isInstanceOf(ArtifactTaskParameters.class);
    ArtifactTaskParameters taskParams = (ArtifactTaskParameters) taskParameters;

    assertThat(taskParams.getAttributes()).isInstanceOf(DockerArtifactDelegateRequest.class);
    DockerArtifactDelegateRequest attributes = (DockerArtifactDelegateRequest) taskParams.getAttributes();
    assertThat(attributes.getImagePath()).isEqualTo("imagePath");
    assertThat(attributes.getTag()).isEqualTo("tag");
  }

  private ArtifactStepParameters getStepParametersForDocker() {
    return ArtifactStepParameters.builder()
        .artifact(DockerHubArtifactConfig.builder()
                      .connectorRef(ParameterField.createValueField("CONNECTOR"))
                      .imagePath(ParameterField.createValueField("imagePath"))
                      .tag(ParameterField.createValueField("tag"))
                      .build())
        .build();
  }

  private ArtifactSourceDelegateRequest getDelegateRequest() {
    return DockerArtifactDelegateRequest.builder()
        .imagePath("imagePath")
        .tag("tag")
        .sourceType(ArtifactSourceType.DOCKER_HUB)
        .dockerConnectorDTO(
            DockerConnectorDTO.builder().dockerRegistryUrl("https://registry.hub.docker.com/v2/").build())
        .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testHandleResultForDocker() {
    DockerArtifactDelegateResponse dockerArtifactAttributes =
        DockerArtifactDelegateResponse.builder().imagePath("imagePath").tag("tag").build();
    ArtifactStepParameters stepParameters = getStepParametersForDocker();
    ArtifactTaskResponse taskResponse =
        ArtifactTaskResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .artifactTaskExecutionResponse(
                ArtifactTaskExecutionResponse.builder().artifactDelegateResponse(dockerArtifactAttributes).build())
            .build();

    StepOutcome stepOutcome = artifactStep.processDelegateResponse(taskResponse, stepParameters);

    assertThat(stepOutcome.getOutcome()).isInstanceOf(DockerArtifactOutcome.class);
    DockerArtifactOutcome outcome = (DockerArtifactOutcome) stepOutcome.getOutcome();
    assertThat(outcome.getImagePath()).isEqualTo("imagePath");
    assertThat(outcome.getTag()).isEqualTo("tag");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldHandleFailTaskResultForDocker() {
    ErrorNotifyResponseData errorNotifyResponseData =
        ErrorNotifyResponseData.builder().errorMessage("Mock error").build();
    ArtifactStepParameters stepParameters = getStepParametersForDocker();
    assertThatThrownBy(() -> artifactStep.processDelegateResponse(errorNotifyResponseData, stepParameters))
        .hasMessageContaining("Mock error");

    // Failure from ArtifactTaskResponse
    ArtifactTaskResponse taskResponse =
        ArtifactTaskResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    assertThatThrownBy(() -> artifactStep.processDelegateResponse(taskResponse, stepParameters))
        .hasMessageContaining("Delegate task failed");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testApplyArtifactOverrides() {
    DockerHubArtifactConfig dockerHubArtifactConfig = DockerHubArtifactConfig.builder()
                                                          .primaryArtifact(true)
                                                          .connectorRef(ParameterField.createValueField("CONNECTOR"))
                                                          .imagePath(ParameterField.createValueField("IMAGE"))
                                                          .tag(ParameterField.createValueField("TAG1"))
                                                          .build();
    DockerHubArtifactConfig dockerHubArtifactConfig2 = DockerHubArtifactConfig.builder()
                                                           .imagePath(ParameterField.createValueField("IMAGE2"))
                                                           .tag(ParameterField.createValueField("TAG2"))
                                                           .build();

    ArtifactStepParameters stepParameters = ArtifactStepParameters.builder()
                                                .artifact(dockerHubArtifactConfig)
                                                .artifactStageOverride(dockerHubArtifactConfig2)
                                                .build();
    ArtifactConfig finalArtifact = artifactStep.applyArtifactsOverlay(stepParameters);
    assertThat(finalArtifact).isInstanceOf(DockerHubArtifactConfig.class);
    DockerHubArtifactConfig artifact = (DockerHubArtifactConfig) finalArtifact;
    assertThat(artifact.isPrimaryArtifact()).isTrue();
    assertThat(artifact.getConnectorRef().getValue()).isEqualTo("CONNECTOR");
    assertThat(artifact.getTag().getValue()).isEqualTo("TAG2");
    assertThat(artifact.getImagePath().getValue()).isEqualTo("IMAGE2");
    assertThat(artifact.getTagRegex()).isEqualTo(null);
  }
}
