package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.CollectionUtils.isPresent;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.PAUSE_ALL_ALREADY;
import static io.harness.exception.WingsException.USER;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSING;

import static java.lang.String.format;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.InterruptUtils;
import io.harness.engine.pms.resume.EngineResumeAllCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
public class PauseAllInterruptHandler implements InterruptHandler {
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  private final Predicate<Interrupt> pauseAllPredicate = interrupt -> interrupt.getType() == InterruptType.PAUSE_ALL;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    return validateAndSave(interrupt);
  }

  private Interrupt validateAndSave(Interrupt interrupt) {
    List<Interrupt> interrupts = interruptService.fetchActiveInterrupts(interrupt.getPlanExecutionId());

    if (interrupt.getNodeExecutionId() != null) {
      // stage interrupt
      validateNodeInterruptOrThrow(interrupts, interrupt);
    } else {
      // plan interrupt
      validatePlanInterruptOrThrow(interrupts);
    }

    PlanExecution planExecution = planExecutionService.get(interrupt.getPlanExecutionId());
    PlanExecution updatedPlanExecution = planExecutionService.updateStatus(planExecution.getUuid(), Status.PAUSING);
    if (updatedPlanExecution == null) {
      log.info("Status for planExecution {} could not be changed to {} on {} interrupt. Current status is {}",
          planExecution.getUuid(), Status.PAUSING, interrupt.getType(), planExecution.getStatus());
    }

    interrupt.setState(PROCESSING);
    if (isEmpty(interrupts)) {
      return interruptService.save(interrupt);
    }

    InterruptUtils
        .obtainOptionalInterruptFromActiveInterruptsWithPredicates(interrupts, interrupt.getPlanExecutionId(),
            interrupt.getNodeExecutionId(),
            ImmutableList.of(presentInterrupt -> presentInterrupt.getType() == InterruptType.RESUME_ALL))
        .ifPresent(resumeAllInterrupt
            -> interruptService.markProcessed(resumeAllInterrupt.getUuid(),
                resumeAllInterrupt.getState() == PROCESSING ? PROCESSED_SUCCESSFULLY : DISCARDED));
    return interruptService.save(interrupt);
  }

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt) {
    throw new UnsupportedOperationException("PAUSE_ALL handling Not required for overall Plan");
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    // Update status
    nodeExecutionService.updateStatusWithOps(nodeExecutionId, Status.PAUSED,
        ops
        -> ops.addToSet(NodeExecutionKeys.interruptHistories,
            InterruptEffect.builder()
                .interruptId(interrupt.getUuid())
                .tookEffectAt(System.currentTimeMillis())
                .interruptType(interrupt.getType())
                .interruptConfig(interrupt.getInterruptConfig())
                .build()),
        EnumSet.noneOf(Status.class));

    waitNotifyEngine.waitForAllOn(
        publisherName, EngineResumeAllCallback.builder().nodeExecutionId(nodeExecutionId).build(), interrupt.getUuid());
    return interrupt;
  }

  private void validateNodeInterruptOrThrow(List<Interrupt> activeInterrupts, Interrupt currentInterrupt) {
    if (isPresent(activeInterrupts, presentInterrupt -> presentInterrupt.getNodeExecutionId() != null)) {
      if (activeInterrupts.stream().anyMatch(presentInterrupt
              -> presentInterrupt.getNodeExecutionId().equals(currentInterrupt.getNodeExecutionId())
                  && pauseAllPredicate.test(presentInterrupt))) {
        throw new InvalidRequestException(
            format("Stage [%s] already has PAUSE_ALL interrupt", currentInterrupt.getNodeExecutionId()),
            PAUSE_ALL_ALREADY, USER);
      }
    }
  }

  private void validatePlanInterruptOrThrow(List<Interrupt> activeInterrupts) {
    if (isPresent(activeInterrupts, pauseAllPredicate)) {
      throw new InvalidRequestException("Execution already has PAUSE_ALL interrupt", PAUSE_ALL_ALREADY, USER);
    }
  }
}
