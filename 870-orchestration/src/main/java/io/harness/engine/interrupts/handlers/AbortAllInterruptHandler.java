package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.isPresent;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.ABORT_ALL_ALREADY;
import static io.harness.exception.WingsException.USER;
import static io.harness.interrupts.Interrupt.State.DISCARDED;
import static io.harness.interrupts.Interrupt.State.PROCESSED_SUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSED_UNSUCCESSFULLY;
import static io.harness.interrupts.Interrupt.State.PROCESSING;
import static io.harness.pms.contracts.execution.Status.DISCONTINUING;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.helpers.AbortHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class AbortAllInterruptHandler implements InterruptHandler {
  @Inject private InterruptService interruptService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AbortHelper abortHelper;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    Interrupt savedInterrupt = validateAndSave(interrupt);
    return isNotEmpty(savedInterrupt.getNodeExecutionId())
        ? handleInterruptForNodeExecution(interrupt, interrupt.getNodeExecutionId())
        : handleInterrupt(savedInterrupt);
  }

  private Interrupt validateAndSave(Interrupt interrupt) {
    return isNotEmpty(interrupt.getNodeExecutionId()) ? validateAndSaveWithNodeExecution(interrupt)
                                                      : validateAndSaveWithoutNodeExecution(interrupt);
  }

  private Interrupt validateAndSaveWithoutNodeExecution(@Valid @NonNull Interrupt interrupt) {
    List<Interrupt> interrupts = interruptService.fetchActiveInterrupts(interrupt.getPlanExecutionId());
    return processInterrupt(interrupt, interrupts);
  }

  private Interrupt validateAndSaveWithNodeExecution(@Valid @NonNull Interrupt interrupt) {
    List<Interrupt> interrupts = interruptService.fetchActiveInterruptsForNodeExecution(
        interrupt.getPlanExecutionId(), interrupt.getNodeExecutionId());
    return processInterrupt(interrupt, interrupts);
  }

  private Interrupt processInterrupt(@Valid @NonNull Interrupt interrupt, List<Interrupt> interrupts) {
    if (isPresent(interrupts, presentInterrupt -> presentInterrupt.getType() == InterruptType.ABORT_ALL)) {
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
    Interrupt updatedInterrupt = interruptService.markProcessing(interrupt.getUuid());
    EnumSet<Status> flowingStatuses = EnumSet.copyOf(StatusUtils.brokeStatuses());
    flowingStatuses.addAll(StatusUtils.finalizableStatuses());
    List<NodeExecution> allNodeExecutions = nodeExecutionService.findAllChildrenWithStatusIn(
        interrupt.getPlanExecutionId(), interrupt.getNodeExecutionId(), flowingStatuses, true);
    List<String> targetIds = allNodeExecutions.stream().map(NodeExecution::getUuid).collect(Collectors.toList());
    if (!abortHelper.markAbortingStateForParent(
            updatedInterrupt, StatusUtils.finalizableStatuses(), allNodeExecutions)) {
      return updatedInterrupt;
    }

    List<NodeExecution> discontinuingNodeExecutions = nodeExecutionService.fetchNodeExecutionsByStatusAndIdIn(
        updatedInterrupt.getPlanExecutionId(), DISCONTINUING, targetIds);

    if (isEmpty(discontinuingNodeExecutions)) {
      log.warn(
          "ABORT_ALL Interrupt being ignored as no running instance found for planExecutionId: {} and nodeExecutionId: {}",
          updatedInterrupt.getUuid(), nodeExecutionId);
      return interruptService.markProcessed(updatedInterrupt.getUuid(), PROCESSED_SUCCESSFULLY);
    }
    return processDiscontinuedInstances(interrupt, updatedInterrupt, discontinuingNodeExecutions);
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
    return processDiscontinuedInstances(interrupt, updatedInterrupt, discontinuingNodeExecutions);
  }

  private Interrupt processDiscontinuedInstances(
      Interrupt interrupt, Interrupt updatedInterrupt, List<NodeExecution> discontinuingNodeExecutions) {
    List<String> notifyIds = new ArrayList<>();
    try {
      for (NodeExecution discontinuingNodeExecution : discontinuingNodeExecutions) {
        abortHelper.discontinueMarkedInstance(discontinuingNodeExecution, updatedInterrupt);
        notifyIds.add(discontinuingNodeExecution.getUuid() + "|" + interrupt.getUuid());
      }

    } catch (Exception ex) {
      interruptService.markProcessed(updatedInterrupt.getUuid(), PROCESSED_UNSUCCESSFULLY);
      throw ex;
    }

    waitNotifyEngine.waitForAllOnInList(
        publisherName, AbortAllInterruptCallback.builder().interrupt(updatedInterrupt).build(), notifyIds);
    return updatedInterrupt;
  }
}
