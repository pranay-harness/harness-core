package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.pms.contracts.execution.Status.ABORTED;
import static io.harness.pms.contracts.execution.Status.RUNNING;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationService;
import io.harness.engine.PlanRepo;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.engine.interrupts.InterruptTestHelper;
import io.harness.engine.interrupts.steps.SimpleAsyncStep;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.rule.Owner;
import io.harness.steps.section.SectionStep;
import io.harness.waiter.OrchestrationNotifyEventListener;

import software.wings.WingsBaseTest;
import software.wings.rules.Listeners;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Listeners(OrchestrationNotifyEventListener.class)
public class AbortAllInterruptHandlerTest extends WingsBaseTest {
  @Inject private Injector injector;
  @Inject private OrchestrationService orchestrationService;
  @Inject private StepRegistry stepRegistry;
  @Inject private InterruptTestHelper interruptTestHelper;
  @Inject private PlanRepo planRepo;

  @Before
  public void setUp() {
    stepRegistry.register(SimpleAsyncStep.STEP_TYPE, injector.getInstance(SimpleAsyncStep.class));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleInterrupt() {
    PlanExecution execution = orchestrationService.startExecution(planRepo.planWithBigWait(), getAbstractions());
    interruptTestHelper.waitForPlanStatus(execution.getUuid(), RUNNING);

    Interrupt handledInterrupt = orchestrationService.registerInterrupt(
        InterruptPackage.builder().planExecutionId(execution.getUuid()).interruptType(ABORT_ALL).build());
    assertThat(handledInterrupt).isNotNull();
    assertThat(handledInterrupt.getState()).isEqualTo(PROCESSED_SUCCESSFULLY);

    interruptTestHelper.waitForPlanCompletion(execution.getUuid());
    PlanExecution abortedExecution = interruptTestHelper.fetchPlanExecutionStatus(execution.getUuid());
    assertThat(abortedExecution).isNotNull();
    assertThat(abortedExecution.getStatus()).isEqualTo(ABORTED);
  }

  private Map<String, String> getAbstractions() {
    return ImmutableMap.of("accountId", generateUuid(), "appId", generateUuid(), "userId", generateUuid(), "userName",
        PRASHANT, "userEmail", PRASHANT);
  }
}
