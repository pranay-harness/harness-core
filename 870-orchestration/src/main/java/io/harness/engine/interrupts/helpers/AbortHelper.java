package io.harness.engine.interrupts.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.contracts.interrupts.InterruptType.ABORT;

import static java.util.stream.Collectors.toList;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionUpdateFailedException;
import io.harness.engine.interrupts.AbortInterruptCallback;
import io.harness.engine.interrupts.InterruptEventQueuePublisher;
import io.harness.engine.interrupts.InterruptProcessingFailedException;
import io.harness.engine.interrupts.handlers.publisher.InterruptEventPublisher;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.EnumSet;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class AbortHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private InterruptEventQueuePublisher interruptEventQueuePublisher;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private InterruptHelper interruptHelper;
  @Inject private InterruptEventPublisher interruptEventPublisher;

  public String discontinueMarkedInstance(NodeExecution nodeExecution, Interrupt interrupt) {
    try {
      boolean taskDiscontinued = interruptHelper.discontinueTaskIfRequired(nodeExecution);
      if (!taskDiscontinued) {
        log.error("Delegate Task Cannot be aborted for NodeExecutionId: {}", nodeExecution.getUuid());
      }
      String notifyId = interruptEventPublisher.publishEvent(nodeExecution.getUuid(), interrupt, ABORT);
      waitNotifyEngine.waitForAllOn(publisherName,
          AbortInterruptCallback.builder()
              .nodeExecutionId(nodeExecution.getUuid())
              .interruptId(interrupt.getUuid())
              .interruptType(interrupt.getType())
              .interruptConfig(interrupt.getInterruptConfig())
              .build(),
          notifyId);
      return notifyId;
    } catch (NodeExecutionUpdateFailedException ex) {
      throw new InterruptProcessingFailedException(InterruptType.ABORT_ALL,
          "Abort failed for execution Plan :" + nodeExecution.getAmbiance().getPlanExecutionId()
              + "for NodeExecutionId: " + nodeExecution.getUuid(),
          ex);
    } catch (Exception e) {
      log.error("Error in discontinuing", e);
      throw new InvalidRequestException("Error in discontinuing, " + e.getMessage());
    }
  }

  public boolean markAbortingState(@NotNull Interrupt interrupt, EnumSet<Status> statuses) {
    // Get all that are eligible for discontinuing
    List<NodeExecution> allNodeExecutions =
        nodeExecutionService.fetchNodeExecutionsByStatuses(interrupt.getPlanExecutionId(), statuses);
    return markAbortingStateForNodes(interrupt, statuses, allNodeExecutions);
  }

  public boolean markAbortingStateForParent(
      @NotNull Interrupt interrupt, EnumSet<Status> statuses, List<NodeExecution> allNodeExecutions) {
    // Get all that are eligible for discontinuing
    return markAbortingStateForNodes(interrupt, statuses, allNodeExecutions);
  }

  private boolean markAbortingStateForNodes(
      @NotNull Interrupt interrupt, EnumSet<Status> statuses, List<NodeExecution> allNodeExecutions) {
    if (isEmpty(allNodeExecutions)) {
      log.warn(
          "No Node Executions could be marked as DISCONTINUING - planExecutionId: {}", interrupt.getPlanExecutionId());
      return false;
    }
    List<String> leafInstanceIds = getAllLeafInstanceIds(interrupt, allNodeExecutions, statuses);
    return nodeExecutionService.markLeavesDiscontinuingOnAbort(
        interrupt.getUuid(), interrupt.getType(), interrupt.getPlanExecutionId(), leafInstanceIds);
  }

  private List<String> getAllLeafInstanceIds(
      Interrupt interrupt, List<NodeExecution> allNodeExecutions, EnumSet<Status> statuses) {
    List<String> allInstanceIds = allNodeExecutions.stream().map(NodeExecution::getUuid).collect(toList());
    // Get Parent Ids
    List<String> parentIds = allNodeExecutions.stream()
                                 .filter(NodeExecution::isChildSpawningMode)
                                 .map(NodeExecution::getUuid)
                                 .collect(toList());
    if (isEmpty(parentIds)) {
      return allInstanceIds;
    }

    List<NodeExecution> children =
        nodeExecutionService.fetchChildrenNodeExecutionsByStatuses(interrupt.getPlanExecutionId(), parentIds, statuses);

    // get distinct parent Ids
    List<String> parentIdsHavingChildren =
        children.stream().map(NodeExecution::getParentId).distinct().collect(toList());

    // parent with no children
    allInstanceIds.removeAll(parentIdsHavingChildren);

    // Mark aborting
    return allInstanceIds;
  }
}
