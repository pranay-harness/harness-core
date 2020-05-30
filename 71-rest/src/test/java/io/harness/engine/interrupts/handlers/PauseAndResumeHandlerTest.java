package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.interrupts.ExecutionInterruptType.PAUSE_ALL;
import static io.harness.interrupts.ExecutionInterruptType.RESUME_ALL;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSING;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.PlanRepo;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.InterruptTestHelper;
import io.harness.engine.interrupts.steps.SimpleAsyncStep;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.ExecutionInstanceStatus;
import io.harness.interrupts.Interrupt;
import io.harness.registries.state.StepRegistry;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.waiter.OrchestrationNotifyEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.rules.Listeners;

import java.util.List;

@Listeners(OrchestrationNotifyEventListener.class)
public class PauseAndResumeHandlerTest extends WingsBaseTest {
  @Inject ExecutionEngine executionEngine;
  @Inject private StepRegistry stepRegistry;
  @Inject private InterruptTestHelper interruptTestHelper;
  @Inject private InterruptManager interruptManager;
  @Inject private InterruptService interruptService;

  private static final EmbeddedUser EMBEDDED_USER = new EmbeddedUser(generateUuid(), PRASHANT, PRASHANT);

  @Before
  public void setUp() {
    stepRegistry.register(SimpleAsyncStep.STEP_TYPE, SimpleAsyncStep.class);
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegisterAndHandleInterrupt() {
    // Execute Plan And wait it to be in RUNNING status
    PlanExecution execution = executionEngine.startExecution(PlanRepo.planWithBigWait(), EMBEDDED_USER);
    interruptTestHelper.waitForPlanStatus(execution.getUuid(), ExecutionInstanceStatus.RUNNING);

    // Issue Pause Interrupt
    Interrupt handledPauseInterrupt = interruptManager.register(execution.getUuid(), PAUSE_ALL, EMBEDDED_USER, null);
    assertThat(handledPauseInterrupt).isNotNull();
    assertThat(handledPauseInterrupt.getState()).isEqualTo(PROCESSING);

    // Wait for Plan To be in PAUSED status
    interruptTestHelper.waitForPlanCompletion(execution.getUuid());

    PlanExecution pausedPlanExecution = interruptTestHelper.fetchPlanExecutionStatus(execution.getUuid());
    assertThat(pausedPlanExecution).isNotNull();
    assertThat(pausedPlanExecution.getStatus()).isEqualTo(ExecutionInstanceStatus.PAUSED);

    // Issue Resume Interrupt
    Interrupt handledResumeInterrupt = interruptManager.register(execution.getUuid(), RESUME_ALL, EMBEDDED_USER, null);
    assertThat(handledResumeInterrupt).isNotNull();

    // Wait for Plan To be complete
    interruptTestHelper.waitForPlanCompletion(execution.getUuid());

    List<Interrupt> allInterrupts = interruptService.fetchAllInterrupts(execution.getUuid());

    assertThat(allInterrupts).isNotEmpty();
    assertThat(allInterrupts).hasSize(2);
    assertThat(allInterrupts.stream().map(Interrupt::getState)).containsExactly(PROCESSING, PROCESSED_SUCCESSFULLY);

    PlanExecution resumedExecution = interruptTestHelper.fetchPlanExecutionStatus(execution.getUuid());
    assertThat(resumedExecution).isNotNull();
    assertThat(resumedExecution.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
  }
}