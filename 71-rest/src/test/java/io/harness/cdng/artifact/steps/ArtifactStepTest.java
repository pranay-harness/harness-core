package io.harness.cdng.artifact.steps;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.ambiance.Ambiance;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskResponse;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.execution.status.Status;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.tasks.Task;
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
  @Spy @Inject @InjectMocks ArtifactStep artifactStep;

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
    DockerArtifactSource source = DockerArtifactSource.builder()
                                      .accountId(ACCOUNT_ID)
                                      .dockerHubConnector("CONNECTOR")
                                      .imagePath("imagePath")
                                      .build();
    doReturn(source).when(artifactStep).getArtifactSource(any(), any());
    Task task = artifactStep.obtainTask(ambiance, stepParameters, null);
    assertThat(task).isInstanceOf(DelegateTask.class);
    DelegateTask delegateTask = (DelegateTask) task;
    assertThat(delegateTask.getAccountId()).isEqualTo(source.getAccountId());
    assertThat(delegateTask.getData().getTaskType()).isEqualTo(TaskType.ARTIFACT_COLLECT_TASK.name());
    assertThat(delegateTask.getData().getParameters()[0]).isInstanceOf(ArtifactTaskParameters.class);
    ArtifactTaskParameters taskParams = (ArtifactTaskParameters) delegateTask.getData().getParameters()[0];

    assertThat(taskParams.getAttributes()).isInstanceOf(DockerArtifactSourceAttributes.class);
    DockerArtifactSourceAttributes attributes = (DockerArtifactSourceAttributes) taskParams.getAttributes();
    assertThat(attributes).isEqualTo(stepParameters.getArtifact().getSourceAttributes());
  }

  private ArtifactStepParameters getStepParametersForDocker() {
    return ArtifactStepParameters.builder()
        .artifact(
            DockerHubArtifactConfig.builder().dockerhubConnector("CONNECTOR").imagePath("imagePath").tag("tag").build())
        .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testHandleResultForDocker() {
    DockerArtifactAttributes dockerArtifactAttributes =
        DockerArtifactAttributes.builder().imagePath("imagePath").tag("tag").dockerHubConnector("CONNECTOR").build();
    ArtifactStepParameters stepParameters = getStepParametersForDocker();
    ArtifactTaskResponse taskResponse = ArtifactTaskResponse.builder()
                                            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                            .artifactAttributes(dockerArtifactAttributes)
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
    DockerHubArtifactConfig dockerHubArtifactConfig = DockerHubArtifactConfig.builder()
                                                          .artifactType(ArtifactUtils.PRIMARY_ARTIFACT)
                                                          .dockerhubConnector("CONNECTOR")
                                                          .imagePath("IMAGE")
                                                          .tag("TAG1")
                                                          .build();
    DockerHubArtifactConfig dockerHubArtifactConfig2 =
        DockerHubArtifactConfig.builder().imagePath("IMAGE2").tag("TAG2").build();

    ArtifactStepParameters stepParameters = ArtifactStepParameters.builder()
                                                .artifact(dockerHubArtifactConfig)
                                                .artifactStageOverride(dockerHubArtifactConfig2)
                                                .build();
    ArtifactConfigWrapper finalArtifact = artifactStep.applyArtifactsOverlay(stepParameters);
    assertThat(finalArtifact).isInstanceOf(DockerHubArtifactConfig.class);
    DockerHubArtifactConfig artifact = (DockerHubArtifactConfig) finalArtifact;
    assertThat(artifact.getArtifactType()).isEqualTo(ArtifactUtils.PRIMARY_ARTIFACT);
    assertThat(artifact.getDockerhubConnector()).isEqualTo("CONNECTOR");
    assertThat(artifact.getTag()).isEqualTo("TAG2");
    assertThat(artifact.getImagePath()).isEqualTo("IMAGE2");
    assertThat(artifact.getTagRegex()).isEqualTo(null);
  }
}