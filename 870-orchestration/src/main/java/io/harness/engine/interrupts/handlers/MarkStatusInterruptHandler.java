package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSED_UNSUCCESSFULLY;
import static io.harness.pms.contracts.execution.Status.INTERVENTION_WAITING;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.execution.Status;

import com.google.inject.Inject;
import javax.validation.Valid;
import lombok.NonNull;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class MarkStatusInterruptHandler implements InterruptHandler {
  @Inject protected NodeExecutionService nodeExecutionService;
  @Inject protected InterruptService interruptService;
  @Inject private OrchestrationEngine orchestrationEngine;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    Interrupt savedInterrupt = validateAndSave(interrupt);
    return handleInterrupt(savedInterrupt);
  }

  private Interrupt validateAndSave(@Valid @NonNull Interrupt interrupt) {
    if (isEmpty(interrupt.getNodeExecutionId())) {
      throw new InvalidRequestException("NodeExecutionId Cannot be empty for MARK_SUCCESS interrupt");
    }

    NodeExecution nodeExecution = nodeExecutionService.get(interrupt.getNodeExecutionId());
    if (nodeExecution.getStatus() != INTERVENTION_WAITING) {
      throw new InvalidRequestException(
          "NodeExecution is not in a finalizable status. Current Status: " + nodeExecution.getStatus());
    }

    interrupt.setState(Interrupt.State.PROCESSING);
    return interruptService.save(interrupt);
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    throw new UnsupportedOperationException(interrupt.getType() + " handling Not required for node individually");
  }

  protected Interrupt handleInterruptStatus(Interrupt interrupt, Status status) {
    try {
      NodeExecution nodeExecution = nodeExecutionService.update(interrupt.getNodeExecutionId(),
          ops
          -> ops.addToSet(NodeExecutionKeys.interruptHistories,
              InterruptEffect.builder()
                  .interruptType(interrupt.getType())
                  .tookEffectAt(System.currentTimeMillis())
                  .interruptId(interrupt.getUuid())
                  .interruptConfig(interrupt.getInterruptConfig())
                  .build()));

      orchestrationEngine.concludeNodeExecution(nodeExecution, status);
    } catch (Exception ex) {
      interruptService.markProcessed(interrupt.getUuid(), PROCESSED_UNSUCCESSFULLY);
      throw ex;
    }
    return interruptService.markProcessed(interrupt.getUuid(), PROCESSED_SUCCESSFULLY);
  }
}
