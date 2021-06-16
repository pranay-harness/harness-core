package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class AbortInterruptHandler implements InterruptHandler {
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AbortHelper abortHelper;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    Interrupt savedInterrupt = interruptService.save(interrupt);
    return handleInterruptForNodeExecution(savedInterrupt, savedInterrupt.getNodeExecutionId());
  }

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt) {
    throw new UnsupportedOperationException("ABORT handling Not required for PLAN");
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    try (AutoLogContext ignore = interrupt.autoLogContext()) {
      NodeExecution nodeExecution = nodeExecutionService.updateStatusWithOps(
          nodeExecutionId, Status.DISCONTINUING, null, EnumSet.noneOf(Status.class));
      if (nodeExecution == null) {
        log.error("Failed to abort node with nodeExecutionId: {}", nodeExecutionId);
        throw new InterruptProcessingFailedException(
            InterruptType.ABORT, "Failed to abort node with nodeExecutionId" + nodeExecutionId);
      }
      abortHelper.discontinueMarkedInstance(nodeExecution, interrupt);
      return interrupt;
    }
  }
}
