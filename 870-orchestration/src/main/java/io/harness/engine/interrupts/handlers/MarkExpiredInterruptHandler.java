package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSED_UNSUCCESSFULLY;
import static io.harness.pms.execution.Status.EXPIRED;

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
import javax.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class MarkExpiredInterruptHandler implements InterruptHandler {
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AbortHelper abortHelper;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    Interrupt savedInterrupt = validateAndSave(interrupt);
    return handleInterrupt(savedInterrupt);
  }

  private Interrupt validateAndSave(@Valid @NonNull Interrupt interrupt) {
    if (isEmpty(interrupt.getNodeExecutionId())) {
      throw new InvalidRequestException("NodeExecutionId Cannot be empty for MARK_EXPIRED interrupt");
    }

    NodeExecution nodeExecution = nodeExecutionService.get(interrupt.getNodeExecutionId());
    if (!StatusUtils.finalizableStatuses().contains(nodeExecution.getStatus())) {
      throw new InvalidRequestException(
          "NodeExecution is not in a finalizable status. Current Status: " + nodeExecution.getStatus());
    }

    interrupt.setState(Interrupt.State.PROCESSING);
    return interruptService.save(interrupt);
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    throw new UnsupportedOperationException("MARK_EXPIRED handling Not required for node individually");
  }

  @Override
  public Interrupt handleInterrupt(@NonNull @Valid Interrupt interrupt) {
    try {
      NodeExecution nodeExecution = nodeExecutionService.get(interrupt.getNodeExecutionId());
      abortHelper.discontinueMarkedInstance(nodeExecution, EXPIRED);
    } catch (Exception ex) {
      interruptService.markProcessed(interrupt.getUuid(), PROCESSED_UNSUCCESSFULLY);
      throw ex;
    }

    return interruptService.markProcessed(interrupt.getUuid(), PROCESSED_SUCCESSFULLY);
  }
}
