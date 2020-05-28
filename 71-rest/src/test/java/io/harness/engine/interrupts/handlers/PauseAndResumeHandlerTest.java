package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.PlanRepo;
import io.harness.engine.interrupts.InterruptTestHelper;
import io.harness.engine.interrupts.steps.SimpleAsyncStep;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.ExecutionInstanceStatus;
import io.harness.interrupts.ExecutionInterruptType;
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

@Listeners(OrchestrationNotifyEventListener.class)
public class PauseAndResumeHandlerTest extends WingsBaseTest {
  @Inject private PauseAllHandler pauseAllHandler;
  @Inject private ResumeAllHandler resumeAllHandler;
  @Inject ExecutionEngine executionEngine;
  @Inject private StepRegistry stepRegistry;
  @Inject private InterruptTestHelper interruptTestHelper;

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
    PlanExecution execution = executionEngine.startExecution(PlanRepo.planWithBigWait(), EMBEDDED_USER);
    Interrupt pauseAllInterrupt = Interrupt.builder()
                                      .uuid(generateUuid())
                                      .planExecutionId(execution.getUuid())
                                      .type(ExecutionInterruptType.PAUSE_ALL)
                                      .createdBy(EMBEDDED_USER)
                                      .build();
    interruptTestHelper.waitForPlanStatus(execution.getUuid(), ExecutionInstanceStatus.RUNNING);
    Interrupt handledPauseInterrupt = pauseAllHandler.registerInterrupt(pauseAllInterrupt);
    assertThat(handledPauseInterrupt).isNotNull();

    interruptTestHelper.waitForPlanCompletion(execution.getUuid());
    PlanExecution abortedExecution = interruptTestHelper.fetchPlanExecutionStatus(execution.getUuid());
    assertThat(abortedExecution).isNotNull();
    assertThat(abortedExecution.getStatus()).isEqualTo(ExecutionInstanceStatus.PAUSED);

    Interrupt resumeAllInterrupt = Interrupt.builder()
                                       .uuid(generateUuid())
                                       .planExecutionId(execution.getUuid())
                                       .type(ExecutionInterruptType.RESUME_ALL)
                                       .createdBy(EMBEDDED_USER)
                                       .build();
    Interrupt handledResumeInterrupt = resumeAllHandler.registerInterrupt(resumeAllInterrupt);
    assertThat(handledResumeInterrupt).isNotNull();

    interruptTestHelper.waitForPlanCompletion(execution.getUuid());
    PlanExecution resumedExecution = interruptTestHelper.fetchPlanExecutionStatus(execution.getUuid());
    assertThat(resumedExecution).isNotNull();
    assertThat(resumedExecution.getStatus()).isEqualTo(ExecutionInstanceStatus.SUCCEEDED);
  }
}