package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.CollectionUtils.isPresent;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.PAUSE_ALL_ALREADY;
import static io.harness.exception.WingsException.USER;
import static io.harness.interrupts.ExecutionInterruptType.PAUSE_ALL;
import static io.harness.interrupts.ExecutionInterruptType.RESUME_ALL;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSING;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.resume.EngineResumeAllCallback;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.status.PausedStepStatusUpdate;
import io.harness.engine.status.StepStatusUpdateInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.InterruptKeys;
import io.harness.interrupts.InterruptEffect;
import io.harness.persistence.HPersistence;
import io.harness.state.io.StepTransput;
import io.harness.waiter.WaitNotifyEngine;

import java.util.List;

public class PauseAllHandler implements InterruptHandler {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private PausedStepStatusUpdate pausedStepStatusUpdate;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    String savedInterruptId = validateAndSave(interrupt);
    return hPersistence.createQuery(Interrupt.class).filter(InterruptKeys.uuid, savedInterruptId).get();
  }

  private String validateAndSave(Interrupt interrupt) {
    List<Interrupt> interrupts = interruptService.fetchActiveInterrupts(interrupt.getPlanExecutionId());
    if (isPresent(interrupts, presentInterrupt -> presentInterrupt.getType() == PAUSE_ALL)) {
      throw new InvalidRequestException("Execution already has PAUSE_ALL interrupt", PAUSE_ALL_ALREADY, USER);
    }
    interrupt.setState(PROCESSING);
    if (isEmpty(interrupts)) {
      return hPersistence.save(interrupt);
    }
    interrupts.stream()
        .filter(presentInterrupt -> presentInterrupt.getType() == RESUME_ALL)
        .findFirst()
        .ifPresent(resumeAllInterrupt
            -> interruptService.markProcessed(resumeAllInterrupt.getUuid(),
                resumeAllInterrupt.getState() == PROCESSING ? PROCESSED_SUCCESSFULLY : DISCARDED));
    return hPersistence.save(interrupt);
  }

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt, Ambiance ambiance, List<StepTransput> additionalInputs) {
    String nodeExecutionId = ambiance.obtainCurrentRuntimeId();

    // Update status
    nodeExecutionService.updateStatusWithOps(nodeExecutionId, Status.PAUSED,
        ops
        -> ops.addToSet(NodeExecutionKeys.interruptHistories,
            InterruptEffect.builder()
                .interruptId(interrupt.getUuid())
                .tookEffectAt(System.currentTimeMillis())
                .interruptType(interrupt.getType())
                .build()));

    pausedStepStatusUpdate.onStepStatusUpdate(StepStatusUpdateInfo.builder()
                                                  .planExecutionId(interrupt.getPlanExecutionId())
                                                  .nodeExecutionId(nodeExecutionId)
                                                  .interruptId(interrupt.getUuid())
                                                  .status(Status.PAUSED)
                                                  .build());
    waitNotifyEngine.waitForAllOn(ORCHESTRATION,
        EngineResumeAllCallback.builder().ambiance(ambiance).additionalInputs(additionalInputs).build(),
        interrupt.getUuid());
    return interrupt;
  }
}
