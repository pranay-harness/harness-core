package io.harness.cdng.k8s;

import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class K8sRollingRollbackStepTest extends CategoryTest {
  private final Ambiance ambiance = Ambiance.newBuilder().build();
  private final StepInputPackage stepInputPackage = StepInputPackage.builder().build();

  @Mock private OutcomeService outcomeService;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputService;
  @InjectMocks private K8sRollingRollbackStep k8sRollingRollbackStep;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSkippingOfRollbackStep() {
    K8sRollingRollbackStepParameters stepParameters = K8sRollingRollbackStepParameters.infoBuilder().build();
    OptionalSweepingOutput optionalSweepingOutput = OptionalSweepingOutput.builder().found(false).build();
    doReturn(optionalSweepingOutput)
        .when(executionSweepingOutputService)
        .resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_ROLL_OUT));

    TaskRequest taskRequest = k8sRollingRollbackStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    assertThat(taskRequest).isNotNull();
    assertThat(taskRequest.getSkipTaskRequest().getMessage())
        .isEqualTo("K8s Rollout Deploy step was not executed. Skipping rollback.");
  }
}
