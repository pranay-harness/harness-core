package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.isPresent;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ABORT_ALL_ALREADY;
import static io.harness.exception.WingsException.USER;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSED_UNSUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSING;
import static io.harness.pms.execution.Status.ABORTED;
import static io.harness.pms.execution.Status.DISCONTINUING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.helpers.AbortHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;
import java.util.List;
import javax.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class AbortAllInterruptHandler implements InterruptHandler {
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AbortHelper abortHelper;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    Interrupt savedInterrupt = validateAndSave(interrupt);
    return handleInterrupt(savedInterrupt);
  }

  private Interrupt validateAndSave(@Valid @NonNull Interrupt interrupt) {
    List<Interrupt> interrupts = interruptService.fetchActiveInterrupts(interrupt.getPlanExecutionId());
    if (isPresent(interrupts, presentInterrupt -> presentInterrupt.getType() == ABORT_ALL)) {
      throw new InvalidRequestException("Execution already has ABORT_ALL interrupt", ABORT_ALL_ALREADY, USER);
    }
    if (isEmpty(interrupts)) {
      return interruptService.save(interrupt);
    }

    interrupts.forEach(savedInterrupt
        -> interruptService.markProcessed(
            savedInterrupt.getUuid(), savedInterrupt.getState() == PROCESSING ? PROCESSED_SUCCESSFULLY : DISCARDED));
    return interruptService.save(interrupt);
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    throw new UnsupportedOperationException("ABORT_ALL handling Not required for node individually");
  }

  @Override
  public Interrupt handleInterrupt(@NonNull @Valid Interrupt interrupt) {
    Interrupt updatedInterrupt = interruptService.markProcessing(interrupt.getUuid());
    if (!abortHelper.markAbortingState(updatedInterrupt, StatusUtils.finalizableStatuses())) {
      return updatedInterrupt;
    }

    List<NodeExecution> discontinuingNodeExecutions =
        nodeExecutionService.fetchNodeExecutionsByStatus(updatedInterrupt.getPlanExecutionId(), DISCONTINUING);

    if (isEmpty(discontinuingNodeExecutions)) {
      log.warn("ABORT_ALL Interrupt being ignored as no running instance found for planExecutionId: {}",
          updatedInterrupt.getUuid());
      return interruptService.markProcessed(updatedInterrupt.getUuid(), PROCESSED_SUCCESSFULLY);
    }
    try {
      for (NodeExecution discontinuingNodeExecution : discontinuingNodeExecutions) {
        abortHelper.discontinueMarkedInstance(discontinuingNodeExecution, ABORTED);
      }

    } catch (Exception ex) {
      interruptService.markProcessed(updatedInterrupt.getUuid(), PROCESSED_UNSUCCESSFULLY);
      throw ex;
    }

    return interruptService.markProcessed(updatedInterrupt.getUuid(), PROCESSED_SUCCESSFULLY);
  }
}
