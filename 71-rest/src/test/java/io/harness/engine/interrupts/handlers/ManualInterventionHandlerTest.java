package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.status.Status.FAILED;
import static io.harness.execution.status.Status.INTERVENTION_WAITING;
import static io.harness.execution.status.Status.SUCCEEDED;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationService;
import io.harness.engine.PlanRepo;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.engine.interrupts.InterruptTestHelper;
import io.harness.engine.interrupts.steps.SimpleAsyncStep;
import io.harness.engine.interrupts.steps.SimpleStepAsyncParams;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.Interrupt;
import io.harness.registries.state.StepRegistry;
import io.harness.rule.Owner;
import io.harness.waiter.OrchestrationNotifyEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.rules.Listeners;

import java.util.List;
import java.util.Map;

@Listeners(OrchestrationNotifyEventListener.class)
public class ManualInterventionHandlerTest extends WingsBaseTest {
  @Inject private OrchestrationService orchestrationService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private InterruptTestHelper interruptTestHelper;
  @Inject private StepRegistry stepRegistry;

  @Before
  public void setUp() {
    stepRegistry.register(SimpleAsyncStep.STEP_TYPE, SimpleAsyncStep.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleInterrupt() {
    PlanExecution execution = orchestrationService.startExecution(PlanRepo.planWithFailure(), getAbstractions());
    interruptTestHelper.waitForPlanStatus(execution.getUuid(), INTERVENTION_WAITING);
    assertThat(execution).isNotNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleInterruptWithRetry() {
    PlanExecution execution = orchestrationService.startExecution(PlanRepo.planWithFailure(), getAbstractions());
    interruptTestHelper.waitForPlanStatus(execution.getUuid(), INTERVENTION_WAITING);
    List<NodeExecution> nodeExecutionList =
        nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(execution.getUuid());

    assertThat(nodeExecutionList).hasSize(1);
    NodeExecution nodeExecution = nodeExecutionList.get(0);
    assertThat(nodeExecution.getStatus()).isEqualTo(INTERVENTION_WAITING);

    Interrupt interrupt =
        orchestrationService.registerInterrupt(InterruptPackage.builder()
                                                   .interruptType(ExecutionInterruptType.RETRY)
                                                   .planExecutionId(execution.getUuid())
                                                   .nodeExecutionId(nodeExecution.getUuid())
                                                   .parameters(SimpleStepAsyncParams.builder().build())
                                                   .build());

    assertThat(interrupt).isNotNull();
    assertThat(interrupt.getUuid()).isNotNull();
    assertThat(interrupt.getState()).isEqualTo(PROCESSED_SUCCESSFULLY);

    interruptTestHelper.waitForPlanStatus(execution.getUuid(), SUCCEEDED);
    nodeExecutionList = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(execution.getUuid());
    assertThat(nodeExecutionList).hasSize(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleInterruptWithMarkSuccess() {
    PlanExecution execution = orchestrationService.startExecution(PlanRepo.planWithFailure(), getAbstractions());
    interruptTestHelper.waitForPlanStatus(execution.getUuid(), INTERVENTION_WAITING);
    List<NodeExecution> nodeExecutionList =
        nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(execution.getUuid());

    assertThat(nodeExecutionList).hasSize(1);
    NodeExecution nodeExecution = nodeExecutionList.get(0);
    assertThat(nodeExecution.getStatus()).isEqualTo(INTERVENTION_WAITING);

    Interrupt interrupt = orchestrationService.registerInterrupt(InterruptPackage.builder()
                                                                     .interruptType(ExecutionInterruptType.MARK_SUCCESS)
                                                                     .planExecutionId(execution.getUuid())
                                                                     .nodeExecutionId(nodeExecution.getUuid())
                                                                     .build());

    assertThat(interrupt).isNotNull();
    assertThat(interrupt.getUuid()).isNotNull();
    assertThat(interrupt.getState()).isEqualTo(PROCESSED_SUCCESSFULLY);

    interruptTestHelper.waitForPlanStatus(execution.getUuid(), SUCCEEDED);
    nodeExecutionList = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(execution.getUuid());
    assertThat(nodeExecutionList).hasSize(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleInterruptWithMarkFailed() {
    PlanExecution execution = orchestrationService.startExecution(PlanRepo.planWithFailure(), getAbstractions());
    interruptTestHelper.waitForPlanStatus(execution.getUuid(), INTERVENTION_WAITING);
    List<NodeExecution> nodeExecutionList =
        nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(execution.getUuid());

    assertThat(nodeExecutionList).hasSize(1);
    NodeExecution nodeExecution = nodeExecutionList.get(0);
    assertThat(nodeExecution.getStatus()).isEqualTo(INTERVENTION_WAITING);

    Interrupt interrupt = orchestrationService.registerInterrupt(InterruptPackage.builder()
                                                                     .interruptType(ExecutionInterruptType.MARK_FAILED)
                                                                     .planExecutionId(execution.getUuid())
                                                                     .nodeExecutionId(nodeExecution.getUuid())
                                                                     .build());

    assertThat(interrupt).isNotNull();
    assertThat(interrupt.getUuid()).isNotNull();
    assertThat(interrupt.getState()).isEqualTo(PROCESSED_SUCCESSFULLY);

    interruptTestHelper.waitForPlanStatus(execution.getUuid(), FAILED);
    nodeExecutionList = nodeExecutionService.fetchNodeExecutionsWithoutOldRetries(execution.getUuid());
    assertThat(nodeExecutionList).hasSize(1);
  }

  private Map<String, String> getAbstractions() {
    return ImmutableMap.of("accountId", generateUuid(), "appId", generateUuid(), "userId", generateUuid(), "userName",
        PRASHANT, "userEmail", PRASHANT);
  }
}
