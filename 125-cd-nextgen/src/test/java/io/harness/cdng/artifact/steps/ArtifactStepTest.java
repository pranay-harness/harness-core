package io.harness.cdng.artifact.steps;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.CategoryTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.utils.ArtifactStepHelper;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.SimpleHDelegateTask;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.execution.status.Status;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;
import io.harness.utils.ParameterField;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.TaskType;

import java.util.HashMap;
import java.util.Map;

public class ArtifactStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS) Ambiance ambiance;
  @Mock ArtifactStepHelper artifactStepHelper;
  @Spy @InjectMocks ArtifactStep artifactStep;

  Map<String, ResponseData> responseDataMap = new HashMap<>();

  @Before
  public void beforeClass() {
    responseDataMap = new HashMap<>();
    when(ambiance.getSetupAbstractions().get("accountId")).thenReturn(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testObtainingArtifactTaskForDocker() {
    ArtifactStepParameters stepParameters = getStepParametersForDocker();
    when(artifactStepHelper.toSourceDelegateRequest(artifactStep.applyArtifactsOverlay(stepParameters), ambiance))
        .thenReturn(getDelegateRequest());
    when(artifactStepHelper.getArtifactStepTaskType(artifactStep.applyArtifactsOverlay(stepParameters)))
        .thenReturn(TaskType.DOCKER_ARTIFACT_TASK_NG.name());
    Task task = artifactStep.obtainTask(ambiance, stepParameters, null);
    assertThat(task).isInstanceOf(SimpleHDelegateTask.class);
    SimpleHDelegateTask delegateTask = (SimpleHDelegateTask) task;
    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegateTask.getData().getTaskType()).isEqualTo(TaskType.DOCKER_ARTIFACT_TASK_NG.name());
    assertThat(delegateTask.getData().getParameters()[0]).isInstanceOf(ArtifactTaskParameters.class);
    ArtifactTaskParameters taskParams = (ArtifactTaskParameters) delegateTask.getData().getParameters()[0];

    assertThat(taskParams.getAttributes()).isInstanceOf(DockerArtifactDelegateRequest.class);
    DockerArtifactDelegateRequest attributes = (DockerArtifactDelegateRequest) taskParams.getAttributes();
    assertThat(attributes.getImagePath()).isEqualTo("imagePath");
    assertThat(attributes.getTag()).isEqualTo("tag");
  }

  private ArtifactStepParameters getStepParametersForDocker() {
    return ArtifactStepParameters.builder()
        .artifact(DockerHubArtifactConfig.builder()
                      .dockerhubConnector(ParameterField.createValueField("CONNECTOR"))
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
    responseDataMap.put("KEY", taskResponse);
    StepResponse stepResponse = artifactStep.handleTaskResult(null, stepParameters, responseDataMap);
    assertThat(stepResponse).isInstanceOf(StepResponse.class);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getStepOutcomes().size()).isEqualTo(1);
    StepOutcome stepOutcome = stepResponse.getStepOutcomes().iterator().next();
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
    responseDataMap.put("KEY", errorNotifyResponseData);
    ArtifactStepParameters stepParameters = getStepParametersForDocker();
    StepResponse stepResponse = artifactStep.handleTaskResult(null, stepParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
    assertThat(stepResponse.getFailureInfo().getErrorMessage()).isEqualTo("Mock error");

    // Failure from ArtifactTaskResponse
    ArtifactTaskResponse taskResponse =
        ArtifactTaskResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
    responseDataMap.put("KEY", taskResponse);
    stepResponse = artifactStep.handleTaskResult(null, stepParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testApplyArtifactOverrides() {
    DockerHubArtifactConfig dockerHubArtifactConfig =
        DockerHubArtifactConfig.builder()
            .primaryArtifact(true)
            .dockerhubConnector(ParameterField.createValueField("CONNECTOR"))
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
    assertThat(artifact.getDockerhubConnector().getValue()).isEqualTo("CONNECTOR");
    assertThat(artifact.getTag().getValue()).isEqualTo("TAG2");
    assertThat(artifact.getImagePath().getValue()).isEqualTo("IMAGE2");
    assertThat(artifact.getTagRegex()).isEqualTo(null);
  }
}