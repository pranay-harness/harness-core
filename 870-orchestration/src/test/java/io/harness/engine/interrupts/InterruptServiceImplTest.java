package io.harness.engine.interrupts;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSING;
import static io.harness.interrupts.Interrupt.State.REGISTERED;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.ExecutionCheck;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.handlers.AbortInterruptHandler;
import io.harness.engine.interrupts.handlers.MarkExpiredInterruptHandler;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class InterruptServiceImplTest extends OrchestrationTestBase {
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private AbortInterruptHandler abortInterruptHandler;
  @Mock private MarkExpiredInterruptHandler markExpiredInterruptHandler;
  @Inject @InjectMocks private InterruptService interruptService;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSave() {
    String planExecutionId = generateUuid();
    Interrupt interrupt = Interrupt.builder().planExecutionId(planExecutionId).type(InterruptType.ABORT_ALL).build();

    Interrupt savedInterrupt = interruptService.save(interrupt);
    assertThat(savedInterrupt).isNotNull();
    assertThat(savedInterrupt.getUuid()).isNotNull();
    assertThat(savedInterrupt.getPlanExecutionId()).isEqualTo(planExecutionId);
    assertThat(savedInterrupt.getType()).isEqualTo(InterruptType.ABORT_ALL);
    assertThat(savedInterrupt.getState()).isEqualTo(REGISTERED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void fetchActivePlanLevelInterrupts() {
    String planExecutionId = generateUuid();
    saveInterruptList(planExecutionId, false);

    List<Interrupt> planLevelInterrupts = interruptService.fetchActivePlanLevelInterrupts(planExecutionId);
    assertThat(planLevelInterrupts).isNotEmpty();
    assertThat(planLevelInterrupts).hasSize(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestMarkProcessed() {
    String planExecutionId = generateUuid();
    Interrupt abortAllInterrupt =
        Interrupt.builder().planExecutionId(planExecutionId).type(InterruptType.ABORT_ALL).build();
    Interrupt savedInterrupt = interruptService.save(abortAllInterrupt);
    Interrupt processed = interruptService.markProcessed(savedInterrupt.getUuid(), DISCARDED);
    assertThat(processed).isNotNull();
    assertThat(processed.getState()).isEqualTo(DISCARDED);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void markProcessing() {
    String planExecutionId = generateUuid();
    Interrupt abortAllInterrupt =
        Interrupt.builder().planExecutionId(planExecutionId).type(InterruptType.ABORT_ALL).build();
    Interrupt savedInterrupt = interruptService.save(abortAllInterrupt);
    Interrupt processing = interruptService.markProcessing(savedInterrupt.getUuid());
    assertThat(processing).isNotNull();
    assertThat(processing.getState()).isEqualTo(PROCESSING);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void fetchAllInterrupts() {
    String planExecutionId = generateUuid();
    saveInterruptList(planExecutionId, false);

    List<Interrupt> interrupts = interruptService.fetchAllInterrupts(planExecutionId);
    assertThat(interrupts).isNotEmpty();
    assertThat(interrupts).hasSize(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void fetchActiveInterrupts() {
    String planExecutionId = generateUuid();
    saveInterruptList(planExecutionId, true);
    List<Interrupt> interrupts = interruptService.fetchActiveInterrupts(planExecutionId);
    assertThat(interrupts).isNotEmpty();
    assertThat(interrupts).hasSize(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testPreInvocationNoInterrupts() {
    String planExecutionId = generateUuid();
    ExecutionCheck executionCheck = interruptService.checkInterruptsPreInvocation(planExecutionId, generateUuid());
    assertThat(executionCheck).isNotNull();
    assertThat(executionCheck.isProceed()).isTrue();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testAbortAllPreInvocationParent() {
    String planExecutionId = generateUuid();
    Interrupt abortAllInterrupt =
        Interrupt.builder().uuid(generateUuid()).planExecutionId(planExecutionId).type(InterruptType.ABORT_ALL).build();
    interruptService.save(abortAllInterrupt);
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(generateUuid())
                                      .status(Status.QUEUED)
                                      .node(PlanNodeProto.newBuilder().setIdentifier(generateUuid()).build())
                                      .ambiance(Ambiance.newBuilder().build())
                                      .mode(ExecutionMode.CHILD)
                                      .version(1L)
                                      .build();
    when(nodeExecutionService.get(nodeExecution.getUuid())).thenReturn(nodeExecution);
    ExecutionCheck executionCheck =
        interruptService.checkInterruptsPreInvocation(planExecutionId, nodeExecution.getUuid());
    assertThat(executionCheck).isNotNull();
    assertThat(executionCheck.isProceed()).isTrue();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testAbortAllPreInvocationNotParent() {
    String planExecutionId = generateUuid();
    Interrupt abortAllInterrupt =
        Interrupt.builder().uuid(generateUuid()).planExecutionId(planExecutionId).type(InterruptType.ABORT_ALL).build();
    interruptService.save(abortAllInterrupt);
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(generateUuid())
                                      .status(Status.QUEUED)
                                      .node(PlanNodeProto.newBuilder().setIdentifier(generateUuid()).build())
                                      .ambiance(Ambiance.newBuilder().build())
                                      .mode(ExecutionMode.TASK)
                                      .version(1L)
                                      .build();
    when(nodeExecutionService.get(nodeExecution.getUuid())).thenReturn(nodeExecution);
    when(abortInterruptHandler.handleInterruptForNodeExecution(any(), eq(nodeExecution.getUuid())))
        .thenReturn(abortAllInterrupt);
    ExecutionCheck executionCheck =
        interruptService.checkInterruptsPreInvocation(planExecutionId, nodeExecution.getUuid());
    assertThat(executionCheck).isNotNull();
    assertThat(executionCheck.isProceed()).isFalse();
    verify(abortInterruptHandler).handleInterruptForNodeExecution(any(), eq(nodeExecution.getUuid()));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testExpireAllPreInvocationNotParent() {
    String planExecutionId = generateUuid();
    Interrupt expireAllInterrupt = Interrupt.builder()
                                       .uuid(generateUuid())
                                       .planExecutionId(planExecutionId)
                                       .type(InterruptType.EXPIRE_ALL)
                                       .build();
    interruptService.save(expireAllInterrupt);
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(generateUuid())
                                      .status(Status.QUEUED)
                                      .node(PlanNodeProto.newBuilder().setIdentifier(generateUuid()).build())
                                      .ambiance(Ambiance.newBuilder().build())
                                      .mode(ExecutionMode.TASK)
                                      .version(1L)
                                      .build();
    when(nodeExecutionService.get(nodeExecution.getUuid())).thenReturn(nodeExecution);
    when(markExpiredInterruptHandler.handleInterruptForNodeExecution(any(), eq(nodeExecution.getUuid())))
        .thenReturn(expireAllInterrupt);
    ExecutionCheck executionCheck =
        interruptService.checkInterruptsPreInvocation(planExecutionId, nodeExecution.getUuid());
    assertThat(executionCheck).isNotNull();
    assertThat(executionCheck.isProceed()).isFalse();
    verify(markExpiredInterruptHandler).handleInterruptForNodeExecution(any(), eq(nodeExecution.getUuid()));
  }

  private void saveInterruptList(String planExecutionId, boolean retryDiscarded) {
    Interrupt abortAllInterrupt =
        Interrupt.builder().planExecutionId(planExecutionId).type(InterruptType.ABORT_ALL).build();
    Interrupt pauseAllInterrupt =
        Interrupt.builder().planExecutionId(planExecutionId).type(InterruptType.PAUSE_ALL).build();
    Interrupt retryInterrupt = Interrupt.builder()
                                   .planExecutionId(planExecutionId)
                                   .type(InterruptType.RETRY)
                                   .nodeExecutionId(generateUuid())
                                   .state(retryDiscarded ? DISCARDED : REGISTERED)
                                   .build();
    interruptService.save(abortAllInterrupt);
    interruptService.save(pauseAllInterrupt);
    interruptService.save(retryInterrupt);
  }
}
