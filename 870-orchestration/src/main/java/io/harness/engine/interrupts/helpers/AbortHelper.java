package io.harness.engine.interrupts.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;

import static java.util.stream.Collectors.toList;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionUpdateFailedException;
import io.harness.engine.interrupts.InterruptCallback;
import io.harness.engine.interrupts.InterruptEventQueuePublisher;
import io.harness.engine.interrupts.InterruptProcessingFailedException;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.interrupts.InterruptEvent;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class AbortHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private Map<TaskCategory, TaskExecutor> taskExecutorMap;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private InterruptEventQueuePublisher interruptEventQueuePublisher;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  public String discontinueMarkedInstance(NodeExecution nodeExecution, Status finalStatus, Interrupt interrupt) {
    try {
      Ambiance ambiance = nodeExecution.getAmbiance();
      // Step currentState = Preconditions.checkNotNull(stepRegistry.obtain(node.getStepType()));
      ExecutableResponse executableResponse = nodeExecution.obtainLatestExecutableResponse();
      if (executableResponse != null && nodeExecution.isTaskSpawningMode()) {
        String taskId;
        TaskCategory taskCategory;
        switch (executableResponse.getResponseCase()) {
          case TASK:
            TaskExecutableResponse taskExecutableResponse = executableResponse.getTask();
            taskId = taskExecutableResponse.getTaskId();
            taskCategory = taskExecutableResponse.getTaskCategory();
            break;
          case TASKCHAIN:
            TaskChainExecutableResponse taskChainExecutableResponse = executableResponse.getTaskChain();
            taskId = taskChainExecutableResponse.getTaskId();
            taskCategory = taskChainExecutableResponse.getTaskCategory();
            break;
          default:
            throw new InvalidRequestException("Executable Response should contain either task or taskChain");
        }
        TaskExecutor executor = taskExecutorMap.get(taskCategory);
        boolean aborted = executor.abortTask(ambiance.getSetupAbstractionsMap(), taskId);
        if (!aborted) {
          log.error(
              "Delegate Task Cannot be aborted : TaskId: {}, NodeExecutionId: {}", taskId, nodeExecution.getUuid());
        }
      }

      InterruptEvent interruptEvent = InterruptEvent.builder()
                                          .interruptUuid(interrupt.getUuid())
                                          .nodeExecution(NodeExecutionMapper.toNodeExecutionProto(nodeExecution))
                                          .interruptType(InterruptType.ABORT)
                                          .build();
      interruptEventQueuePublisher.send(nodeExecution.getNode().getServiceName(), interruptEvent);

      waitNotifyEngine.waitForAllOn(publisherName,
          InterruptCallback.builder()
              .finalStatus(finalStatus)
              .nodeExecutionId(nodeExecution.getUuid())
              .interruptId(interrupt.getUuid())
              .build(),
          interruptEvent.getNotifyId());
      return interruptEvent.getNotifyId();
    } catch (NodeExecutionUpdateFailedException ex) {
      throw new InterruptProcessingFailedException(ABORT_ALL,
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
