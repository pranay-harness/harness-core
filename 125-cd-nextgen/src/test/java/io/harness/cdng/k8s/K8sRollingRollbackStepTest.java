package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.k8s.beans.K8sRollingReleaseOutput;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingRollbackDeployRequest;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class K8sRollingRollbackStepTest extends CategoryTest {
  private final Ambiance ambiance = Ambiance.newBuilder().build();
  private final StepInputPackage stepInputPackage = StepInputPackage.builder().build();

  @Mock private OutcomeService outcomeService;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputService;
  @Mock private K8sStepHelper k8sStepHelper;
  @InjectMocks private K8sRollingRollbackStep k8sRollingRollbackStep;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSkippingOfRollbackStep() {
    String rollingFqn = "pipeline.stages.deploy.spec.execution.steps.rolloutDeployment";
    K8sRollingRollbackStepParameters stepParameters =
        K8sRollingRollbackStepParameters.infoBuilder().rollingStepFqn(rollingFqn).build();
    final StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    OptionalSweepingOutput optionalSweepingOutput = OptionalSweepingOutput.builder().found(false).build();
    doReturn(optionalSweepingOutput)
        .when(executionSweepingOutputService)
        .resolveOptional(ambiance,
            RefObjectUtils.getSweepingOutputRefObject(rollingFqn + "." + K8sRollingReleaseOutput.OUTPUT_NAME));

    TaskRequest taskRequest = k8sRollingRollbackStep.obtainTask(ambiance, stepElementParameters, stepInputPackage);
    assertThat(taskRequest).isNotNull();
    assertThat(taskRequest.getSkipTaskRequest().getMessage())
        .isEqualTo("K8s Rollout Deploy step was not executed. Skipping rollback.");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRollback() {
    OptionalSweepingOutput releaseOutput = OptionalSweepingOutput.builder()
                                               .found(true)
                                               .output(K8sRollingReleaseOutput.builder().name("test").build())
                                               .build();
    OptionalSweepingOutput deploymentOutput =
        OptionalSweepingOutput.builder()
            .found(true)
            .output(K8sRollingOutcome.builder().releaseName("test").releaseNumber(3).build())
            .build();

    testRollback(releaseOutput, deploymentOutput, "test", 3);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRollbackNoRollingOutcome() {
    OptionalSweepingOutput releaseOutput = OptionalSweepingOutput.builder()
                                               .found(true)
                                               .output(K8sRollingReleaseOutput.builder().name("test").build())
                                               .build();
    OptionalSweepingOutput deploymentOutput = OptionalSweepingOutput.builder().found(false).build();

    testRollback(releaseOutput, deploymentOutput, "test", null);
  }

  private void testRollback(OptionalSweepingOutput releaseOutput, OptionalSweepingOutput deploymentOutput,
      String expectedReleaseName, Integer expectedReleaseNumber) {
    String rollingFqn = "pipeline.stages.deploy.spec.execution.steps.rolloutDeployment";
    final K8sRollingRollbackStepParameters stepParameters =
        K8sRollingRollbackStepParameters.infoBuilder().rollingStepFqn(rollingFqn).build();
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("10m")).build();
    final TaskRequest taskRequest = TaskRequest.newBuilder().build();
    final TaskChainResponse taskChainResponse = TaskChainResponse.builder().taskRequest(taskRequest).build();

    doReturn(releaseOutput)
        .when(executionSweepingOutputService)
        .resolveOptional(ambiance,
            RefObjectUtils.getSweepingOutputRefObject(rollingFqn + "." + K8sRollingReleaseOutput.OUTPUT_NAME));
    doReturn(deploymentOutput)
        .when(executionSweepingOutputService)
        .resolveOptional(ambiance,
            RefObjectUtils.getSweepingOutputRefObject(rollingFqn + "." + OutcomeExpressionConstants.K8S_ROLL_OUT));

    doReturn(taskChainResponse)
        .when(k8sStepHelper)
        .queueK8sTask(eq(stepElementParameters), any(K8sDeployRequest.class), eq(ambiance),
            any(K8sExecutionPassThroughData.class));

    TaskRequest result = k8sRollingRollbackStep.obtainTask(ambiance, stepElementParameters, stepInputPackage);
    assertThat(result).isNotNull();
    assertThat(result).isSameAs(taskRequest);

    ArgumentCaptor<K8sDeployRequest> requestArgumentCaptor = ArgumentCaptor.forClass(K8sDeployRequest.class);
    verify(k8sStepHelper, times(1))
        .queueK8sTask(eq(stepElementParameters), requestArgumentCaptor.capture(), eq(ambiance),
            any(K8sExecutionPassThroughData.class));
    K8sRollingRollbackDeployRequest request = (K8sRollingRollbackDeployRequest) requestArgumentCaptor.getValue();
    assertThat(request.getReleaseName()).isEqualTo(expectedReleaseName);
    assertThat(request.getReleaseNumber()).isEqualTo(expectedReleaseNumber);
  }
}
