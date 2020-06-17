package io.harness.engine.helpers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.status.Status.DISCONTINUING;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;
import static java.util.stream.Collectors.toList;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.mongodb.client.result.UpdateResult;
import io.harness.ambiance.Ambiance;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.interrupts.InterruptProcessingFailedException;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.NodeExecutionUpdateFailedException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.Abortable;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.facilitator.modes.TaskSpawningExecutableResponse;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.plan.PlanNode;
import io.harness.registries.state.StepRegistry;
import io.harness.state.Step;
import io.harness.tasks.TaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Slf4j
public class AbortHelper {
  @Inject private StepRegistry stepRegistry;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private Map<String, TaskExecutor> taskExecutorMap;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private ExecutionEngine engine;

  public void discontinueMarkedInstance(NodeExecution nodeExecution, Status finalStatus) {
    try {
      Ambiance ambiance = ambianceHelper.fetchAmbiance(nodeExecution);
      PlanNode node = nodeExecution.getNode();
      Step currentState = Preconditions.checkNotNull(stepRegistry.obtain(node.getStepType()));
      ExecutableResponse executableResponse = nodeExecution.obtainLatestExecutableResponse();
      if (executableResponse != null && nodeExecution.isTaskSpawningMode()) {
        TaskSpawningExecutableResponse taskExecutableResponse = (TaskSpawningExecutableResponse) executableResponse;
        TaskExecutor executor = taskExecutorMap.get(taskExecutableResponse.getTaskIdentifier());
        executor.abortTask(ambiance, taskExecutableResponse.getTaskId());
      }
      if (currentState instanceof Abortable) {
        ((Abortable) currentState).handleAbort(ambiance, nodeExecution.getResolvedStepParameters(), executableResponse);
      }

      NodeExecution updatedNodeExecution = nodeExecutionService.update(nodeExecution.getUuid(),
          ops
          -> ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis()).set(NodeExecutionKeys.status, finalStatus));
      engine.endTransition(updatedNodeExecution, finalStatus, null, null);
    } catch (NodeExecutionUpdateFailedException ex) {
      throw new InterruptProcessingFailedException(ABORT_ALL,
          "Abort failed for execution Plan :" + nodeExecution.getPlanExecutionId()
              + "for NodeExecutionId: " + nodeExecution.getUuid(),
          ex);
    } catch (Exception e) {
      logger.error("Error in discontinuing", e);
    }
  }

  public boolean markAbortingState(@NotNull Interrupt interrupt, EnumSet<Status> statuses) {
    // Get all that are eligible for discontinuing
    List<NodeExecution> allNodeExecutions =
        nodeExecutionService.fetchNodeExecutionsByStatuses(interrupt.getPlanExecutionId(), statuses);
    if (isEmpty(allNodeExecutions)) {
      logger.warn(
          "No Node Executions could be marked as DISCONTINUING - planExecutionId: {}", interrupt.getPlanExecutionId());
      return false;
    }
    List<String> leafInstanceIds = getAllLeafInstanceIds(interrupt, allNodeExecutions, statuses);
    Update ops = new Update();
    ops.set(NodeExecutionKeys.status, DISCONTINUING);
    ops.addToSet(NodeExecutionKeys.interruptHistories,
        InterruptEffect.builder()
            .interruptId(interrupt.getUuid())
            .tookEffectAt(System.currentTimeMillis())
            .interruptType(interrupt.getType())
            .build());

    Query query = query(where(NodeExecutionKeys.planExecutionId).is(interrupt.getPlanExecutionId()))
                      .addCriteria(where(NodeExecutionKeys.uuid).in(leafInstanceIds));
    UpdateResult updateResult = mongoTemplate.updateMulti(query, ops, NodeExecution.class);
    if (!updateResult.wasAcknowledged()) {
      logger.warn(
          "No NodeExecutions could be marked as DISCONTINUING -  planExecutionId: {}", interrupt.getPlanExecutionId());
      return false;
    }
    return true;
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
