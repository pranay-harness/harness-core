package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.CollectionUtils.isPresent;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSING;
import static io.harness.pms.contracts.execution.Status.APPROVAL_WAITING;
import static io.harness.pms.contracts.execution.Status.ASYNC_WAITING;
import static io.harness.pms.contracts.execution.Status.INTERVENTION_WAITING;
import static io.harness.pms.contracts.execution.Status.PAUSING;
import static io.harness.pms.contracts.execution.Status.RESOURCE_WAITING;
import static io.harness.pms.contracts.execution.Status.RUNNING;
import static io.harness.pms.contracts.execution.Status.TASK_WAITING;
import static io.harness.pms.contracts.execution.Status.TIMED_WAITING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptProcessingFailedException;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.helpers.AbortHelper;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.List;
import javax.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@OwnedBy(PIPELINE)
public class AbortInterruptHandler implements InterruptHandler {
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AbortHelper abortHelper;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    Interrupt savedInterrupt = validateAndSave(interrupt);
    return handleInterruptForNodeExecution(savedInterrupt, savedInterrupt.getNodeExecutionId());
  }

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt) {
    throw new UnsupportedOperationException("ABORT handling Not required for PLAN");
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(@NotNull Interrupt interrupt, @NotNull String nodeExecutionId) {
    try (AutoLogContext ignore = interrupt.autoLogContext()) {
      NodeExecution nodeExecution =
          nodeExecutionService.updateStatusWithOps(nodeExecutionId, Status.DISCONTINUING, null,
              EnumSet.of(RUNNING, INTERVENTION_WAITING, TIMED_WAITING, ASYNC_WAITING, TASK_WAITING, PAUSING,
                  RESOURCE_WAITING, APPROVAL_WAITING));

      if (nodeExecution == null) {
        log.error("Failed to abort node with nodeExecutionId: {}", nodeExecutionId);
        throw new InterruptProcessingFailedException(
            InterruptType.ABORT, "Failed to abort node with nodeExecutionId" + nodeExecutionId);
      }
      abortHelper.discontinueMarkedInstance(nodeExecution, interrupt);
      return interrupt;
    }
  }

  private Interrupt validateAndSave(Interrupt interrupt) {
    if (isEmpty(interrupt.getNodeExecutionId())) {
      log.error("Failed to abort node with nodeExecutionId: null. NodeExecutionId cannot be null");
      throw new InterruptProcessingFailedException(
          InterruptType.ABORT, "Failed to abort node with nodeExecutionId: null. NodeExecutionId cannot be null");
    }

    List<Interrupt> interrupts = interruptService.fetchActiveInterruptsForNodeExecution(
        interrupt.getPlanExecutionId(), interrupt.getNodeExecutionId());
    return processInterrupt(interrupt, interrupts);
  }

  private Interrupt processInterrupt(@Valid @NonNull Interrupt interrupt, List<Interrupt> interrupts) {
    if (isPresent(interrupts, presentInterrupt -> presentInterrupt.getType() == InterruptType.ABORT)) {
      throw new InterruptProcessingFailedException(InterruptType.ABORT,
          "Execution already contains ABORT interrupt for nodeExecution: " + interrupt.getNodeExecutionId());
    }
    if (isEmpty(interrupts)) {
      return interruptService.save(interrupt);
    }

    interrupts.forEach(savedInterrupt
        -> interruptService.markProcessed(
            savedInterrupt.getUuid(), savedInterrupt.getState() == PROCESSING ? PROCESSED_SUCCESSFULLY : DISCARDED));
    return interruptService.save(interrupt);
  }
}
