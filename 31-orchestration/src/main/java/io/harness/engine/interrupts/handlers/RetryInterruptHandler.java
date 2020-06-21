package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.status.Status.retryableStatuses;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.helpers.RetryHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.State;

@OwnedBy(CDC)
public class RetryInterruptHandler implements InterruptHandler {
  @Inject private RetryHelper retryHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private InterruptService interruptService;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    Interrupt savedInterrupt = validateAndSave(interrupt);
    return handleInterrupt(savedInterrupt);
  }

  private Interrupt validateAndSave(Interrupt interrupt) {
    if (isEmpty(interrupt.getNodeExecutionId())) {
      throw new InvalidRequestException("NodeExecutionId Cannot be empty for RETRY interrupt");
    }
    NodeExecution nodeExecution = nodeExecutionService.get(interrupt.getNodeExecutionId());
    if (!retryableStatuses().contains(nodeExecution.getStatus())) {
      throw new InvalidRequestException(
          "NodeExecution is not in a retryable status. Current Status: " + nodeExecution.getStatus());
    }
    interrupt.setState(State.PROCESSING);
    return interruptService.save(interrupt);
  }

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt) {
    retryHelper.retryNodeExecution(interrupt.getNodeExecutionId());
    return interruptService.markProcessed(interrupt.getUuid(), State.PROCESSED_SUCCESSFULLY);
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    throw new UnsupportedOperationException("Please use handleInterrupt for handling retries");
  }
}
