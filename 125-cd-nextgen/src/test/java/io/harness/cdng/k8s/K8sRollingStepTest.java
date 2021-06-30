package io.harness.cdng.k8s;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.GeneralException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDP)
public class K8sRollingStepTest extends AbstractK8sStepExecutorTestBase {
  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @InjectMocks private K8sRollingStep k8sRollingStep;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteK8sTask() {
    K8sRollingStepParameters stepParameters = new K8sRollingStepParameters();
    stepParameters.setSkipDryRun(ParameterField.createValueField(true));
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();

    when(executionSweepingOutputService.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.K8S_CANARY_OUTCOME)))
        .thenReturn(OptionalSweepingOutput.builder().found(false).build());

    K8sRollingDeployRequest request = executeTask(stepElementParameters, K8sRollingDeployRequest.class);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.DEPLOYMENT_ROLLING);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.isSkipResourceVersioning()).isTrue();
    assertThat(request.isInCanaryWorkflow()).isFalse();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteK8sTaskInCanaryWorkflow() {
    K8sRollingStepParameters stepParameters = new K8sRollingStepParameters();
    stepParameters.setSkipDryRun(ParameterField.createValueField(true));
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();

    when(executionSweepingOutputService.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.K8S_CANARY_OUTCOME)))
        .thenReturn(OptionalSweepingOutput.builder().found(true).build());

    K8sRollingDeployRequest request = executeTask(stepElementParameters, K8sRollingDeployRequest.class);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.DEPLOYMENT_ROLLING);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.isSkipResourceVersioning()).isTrue();
    assertThat(request.isInCanaryWorkflow()).isTrue();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteK8sTaskNullParameterFields() {
    K8sRollingStepParameters stepParameters = new K8sRollingStepParameters();
    stepParameters.setSkipDryRun(ParameterField.ofNull());
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.ofNull()).build();

    when(executionSweepingOutputService.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.K8S_CANARY_OUTCOME)))
        .thenReturn(OptionalSweepingOutput.builder().found(false).build());

    K8sRollingDeployRequest request = executeTask(stepElementParameters, K8sRollingDeployRequest.class);
    assertThat(request.isSkipDryRun()).isFalse();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(K8sStepHelper.getTimeoutInMin(stepElementParameters));
    assertThat(request.isSkipResourceVersioning()).isTrue();
  }

  @SneakyThrows
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testOutcomesInResponse() {
    K8sRollingStepParameters stepParameters = new K8sRollingStepParameters();
    final StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    K8sDeployResponse k8sDeployResponse =
        K8sDeployResponse.builder()
            .k8sNGTaskResponse(K8sRollingDeployResponse.builder().releaseNumber(1).build())
            .commandUnitsProgress(UnitProgressData.builder().build())
            .commandExecutionStatus(SUCCESS)
            .build();
    when(k8sStepHelper.getReleaseName(any())).thenReturn("releaseName");

    StepResponse response = k8sRollingStep.finalizeExecutionWithSecurityContext(
        ambiance, stepElementParameters, K8sExecutionPassThroughData.builder().build(), () -> k8sDeployResponse);
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(response.getStepOutcomes()).hasSize(1);

    StepOutcome outcome = response.getStepOutcomes().stream().collect(Collectors.toList()).get(0);
    assertThat(outcome.getOutcome()).isInstanceOf(K8sRollingOutcome.class);
    assertThat(outcome.getName()).isEqualTo(OutcomeExpressionConstants.OUTPUT);
    assertThat(outcome.getGroup()).isNull();

    ArgumentCaptor<K8sRollingOutcome> argumentCaptor = ArgumentCaptor.forClass(K8sRollingOutcome.class);
    verify(executionSweepingOutputService, times(1))
        .consume(eq(ambiance), eq(OutcomeExpressionConstants.K8S_ROLL_OUT), argumentCaptor.capture(),
            eq(StepOutcomeGroup.STAGE.name()));
    assertThat(argumentCaptor.getValue().getReleaseName()).isEqualTo("releaseName");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFinalizeExecutionException() {
    final StepElementParameters stepElementParameters = StepElementParameters.builder().build();
    final Exception thrownException = new GeneralException("Something went wrong");
    final K8sExecutionPassThroughData executionPassThroughData = K8sExecutionPassThroughData.builder().build();
    final StepResponse stepResponse = StepResponse.builder().status(Status.FAILED).build();

    doReturn(stepResponse).when(k8sStepHelper).handleTaskException(ambiance, executionPassThroughData, thrownException);

    StepResponse response = k8sRollingStep.finalizeExecutionWithSecurityContext(
        ambiance, stepElementParameters, executionPassThroughData, () -> { throw thrownException; });

    assertThat(response).isEqualTo(stepResponse);

    verify(k8sStepHelper, times(1)).handleTaskException(ambiance, executionPassThroughData, thrownException);
  }

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return k8sRollingStep;
  }
}
