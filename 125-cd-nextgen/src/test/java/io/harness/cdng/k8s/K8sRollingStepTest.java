package io.harness.cdng.k8s;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.steps.StepOutcomeGroup;

import java.util.stream.Collectors;
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
    stepParameters.setTimeout(ParameterField.createValueField("30m"));

    K8sRollingDeployRequest request = executeTask(stepParameters, K8sRollingDeployRequest.class);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.DEPLOYMENT_ROLLING);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.isSkipResourceVersioning()).isTrue();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteK8sTaskNullParameterFields() {
    K8sRollingStepParameters stepParameters = new K8sRollingStepParameters();
    stepParameters.setSkipDryRun(ParameterField.ofNull());
    stepParameters.setTimeout(ParameterField.ofNull());

    K8sRollingDeployRequest request = executeTask(stepParameters, K8sRollingDeployRequest.class);
    assertThat(request.isSkipDryRun()).isFalse();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(K8sStepHelper.getTimeout(stepParameters));
    assertThat(request.isSkipResourceVersioning()).isTrue();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testOutcomesInResponse() {
    K8sRollingStepParameters stepParameters = new K8sRollingStepParameters();

    K8sDeployResponse k8sDeployResponse =
        K8sDeployResponse.builder()
            .k8sNGTaskResponse(K8sRollingDeployResponse.builder().releaseNumber(1).build())
            .commandUnitsProgress(UnitProgressData.builder().build())
            .commandExecutionStatus(SUCCESS)
            .build();
    when(k8sStepHelper.getReleaseName(any())).thenReturn("releaseName");

    StepResponse response = k8sRollingStep.finalizeExecution(ambiance, stepParameters, null, () -> k8sDeployResponse);
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

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return k8sRollingStep;
  }
}
