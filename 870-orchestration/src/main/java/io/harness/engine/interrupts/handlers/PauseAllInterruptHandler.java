package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.isPresent;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.PAUSE_ALL_ALREADY;
import static io.harness.exception.WingsException.USER;
import static io.harness.interrupts.ExecutionInterruptType.PAUSE_ALL;
import static io.harness.interrupts.ExecutionInterruptType.RESUME_ALL;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSING;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventEmitter;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptHelper;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.statusupdate.PausedStepStatusUpdate;
import io.harness.engine.interrupts.statusupdate.StepStatusUpdateInfo;
import io.harness.engine.resume.EngineResumeAllCallback;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;

@OwnedBy(CDC)
public class PauseAllInterruptHandler implements InterruptHandler {
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private PausedStepStatusUpdate pausedStepStatusUpdate;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private OrchestrationEventEmitter eventEmitter;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    return validateAndSave(interrupt);
  }

  private Interrupt validateAndSave(Interrupt interrupt) {
    List<Interrupt> interrupts = interruptService.fetchActiveInterrupts(interrupt.getPlanExecutionId());
    if (isPresent(interrupts, presentInterrupt -> presentInterrupt.getType() == PAUSE_ALL)) {
      throw new InvalidRequestException("Execution already has PAUSE_ALL interrupt", PAUSE_ALL_ALREADY, USER);
    }

    PlanExecution planExecution = planExecutionService.get(interrupt.getPlanExecutionId());
    planExecutionService.updateStatus(planExecution.getUuid(), Status.PAUSING);
    eventEmitter.emitEvent(OrchestrationEvent.builder()
                               .ambiance(InterruptHelper.buildFromPlanExecution(planExecution))
                               .eventType(OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE)
                               .build());

    interrupt.setState(PROCESSING);
    if (isEmpty(interrupts)) {
      return interruptService.save(interrupt);
    }
    interrupts.stream()
        .filter(presentInterrupt -> presentInterrupt.getType() == RESUME_ALL)
        .findFirst()
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
                .build()));

    pausedStepStatusUpdate.onStepStatusUpdate(StepStatusUpdateInfo.builder()
                                                  .planExecutionId(interrupt.getPlanExecutionId())
                                                  .nodeExecutionId(nodeExecutionId)
                                                  .interruptId(interrupt.getUuid())
                                                  .status(Status.PAUSED)
                                                  .build());
    waitNotifyEngine.waitForAllOn(
        publisherName, EngineResumeAllCallback.builder().nodeExecutionId(nodeExecutionId).build(), interrupt.getUuid());
    return interrupt;
  }
}
