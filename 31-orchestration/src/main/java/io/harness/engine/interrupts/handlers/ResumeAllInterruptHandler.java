package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.CollectionUtils.filterAndGetFirst;
import static io.harness.data.structure.CollectionUtils.isPresent;
import static io.harness.eraro.ErrorCode.RESUME_ALL_ALREADY;
import static io.harness.exception.WingsException.USER;
import static io.harness.interrupts.ExecutionInterruptType.PAUSE_ALL;
import static io.harness.interrupts.ExecutionInterruptType.RESUME_ALL;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSING;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.status.ResumeStepStatusUpdate;
import io.harness.engine.status.StepStatusUpdateInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.InterruptKeys;
import io.harness.interrupts.InterruptEffect;
import io.harness.persistence.HPersistence;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;

import java.util.List;
import java.util.Optional;

public class ResumeAllInterruptHandler implements InterruptHandler {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private ResumeStepStatusUpdate resumeStepStatusUpdate;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    String savedInterruptId = validateAndSave(interrupt);
    return hPersistence.createQuery(Interrupt.class).filter(InterruptKeys.uuid, savedInterruptId).get();
  }

  private String validateAndSave(Interrupt interrupt) {
    List<Interrupt> interrupts = interruptService.fetchActiveInterrupts(interrupt.getPlanExecutionId());
    Optional<Interrupt> pauseAllOptional =
        filterAndGetFirst(interrupts, presentInterrupt -> presentInterrupt.getType() == PAUSE_ALL);
    if (!pauseAllOptional.isPresent()) {
      throw new InvalidRequestException("No PAUSE_ALL interrupt present", USER);
    }

    if (isPresent(interrupts, presentInterrupt -> presentInterrupt.getType() == RESUME_ALL)) {
      throw new InvalidRequestException("Interrupt RESUME_ALL already present", RESUME_ALL_ALREADY, USER);
    }
    Interrupt pauseAllInterrupt = pauseAllOptional.get();
    interruptService.markProcessed(
        pauseAllInterrupt.getUuid(), pauseAllInterrupt.getState() == PROCESSING ? PROCESSED_SUCCESSFULLY : DISCARDED);
    waitNotifyEngine.doneWith(
        pauseAllInterrupt.getUuid(), StatusNotifyResponseData.builder().status(Status.SUCCEEDED).build());
    interrupt.setState(PROCESSING);
    return hPersistence.save(interrupt);
  }

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt) {
    throw new UnsupportedOperationException("RESUME_ALL handling Not required for overall Plan");
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution.getStatus() != Status.PAUSED) {
      return interrupt;
    }
    // Update status
    nodeExecutionService.updateStatusWithOps(nodeExecutionId, Status.QUEUED,
        ops
        -> ops.addToSet(NodeExecutionKeys.interruptHistories,
            InterruptEffect.builder()
                .interruptId(interrupt.getUuid())
                .tookEffectAt(System.currentTimeMillis())
                .interruptType(interrupt.getType())
                .build()));

    resumeStepStatusUpdate.onStepStatusUpdate(StepStatusUpdateInfo.builder()
                                                  .planExecutionId(interrupt.getPlanExecutionId())
                                                  .nodeExecutionId(nodeExecutionId)
                                                  .interruptId(interrupt.getUuid())
                                                  .status(Status.QUEUED)
                                                  .build());
    return interrupt;
  }
}
