package io.harness.engine.interrupts;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;
import static io.harness.interrupts.ExecutionInterruptType.PAUSE_ALL;
import static io.harness.interrupts.ExecutionInterruptType.RETRY;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSING;
import static io.harness.interrupts.Interrupt.State.REGISTERED;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.interrupts.Interrupt;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class InterruptServiceImplTest extends OrchestrationTest {
  @Inject private InterruptService interruptService;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSave() {
    String planExecutionId = generateUuid();
    Interrupt interrupt = Interrupt.builder().planExecutionId(planExecutionId).type(ABORT_ALL).build();

    Interrupt savedInterrupt = interruptService.save(interrupt);
    assertThat(savedInterrupt).isNotNull();
    assertThat(savedInterrupt.getUuid()).isNotNull();
    assertThat(savedInterrupt.getCreatedAt()).isNotNull();
    assertThat(savedInterrupt.getLastUpdatedAt()).isNotNull();
    assertThat(savedInterrupt.getPlanExecutionId()).isEqualTo(planExecutionId);
    assertThat(savedInterrupt.getType()).isEqualTo(ABORT_ALL);
    assertThat(savedInterrupt.getState()).isEqualTo(REGISTERED);
    assertThat(savedInterrupt.getCreatedAt()).isNotNull();
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void fetchActivePlanLevelInterrupts() {
    String planExecutionId = generateUuid();
    saveInterruptList(planExecutionId, false);

    List<Interrupt> planLevelInterrupts = interruptService.fetchActivePlanLevelInterrupts(planExecutionId);
    assertThat(planLevelInterrupts).isNotEmpty();
    assertThat(planLevelInterrupts).hasSize(1);
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestMarkProcessed() {
    String planExecutionId = generateUuid();
    Interrupt abortAllInterrupt = Interrupt.builder().planExecutionId(planExecutionId).type(ABORT_ALL).build();
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
    Interrupt abortAllInterrupt = Interrupt.builder().planExecutionId(planExecutionId).type(ABORT_ALL).build();
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

  private void saveInterruptList(String planExecutionId, boolean retryDiscarded) {
    Interrupt abortAllInterrupt = Interrupt.builder().planExecutionId(planExecutionId).type(ABORT_ALL).build();
    Interrupt pauseAllInterrupt = Interrupt.builder().planExecutionId(planExecutionId).type(PAUSE_ALL).build();
    Interrupt retryInterrupt = Interrupt.builder()
                                   .planExecutionId(planExecutionId)
                                   .type(RETRY)
                                   .nodeExecutionId(generateUuid())
                                   .state(retryDiscarded ? DISCARDED : REGISTERED)
                                   .build();
    interruptService.save(abortAllInterrupt);
    interruptService.save(pauseAllInterrupt);
    interruptService.save(retryInterrupt);
  }
}