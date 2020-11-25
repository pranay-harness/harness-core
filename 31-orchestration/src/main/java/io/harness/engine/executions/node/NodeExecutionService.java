package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.pms.execution.Status;
import io.harness.state.io.StepParameters;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(CDC)
public interface NodeExecutionService {
  NodeExecution get(String nodeExecutionId);

  NodeExecution getByPlanNodeUuid(String planNodeUuid, String planExecutionId);

  List<NodeExecution> fetchNodeExecutions(String planExecutionId);

  List<NodeExecution> fetchNodeExecutionsWithoutOldRetries(String planExecutionId);

  List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId);

  List<NodeExecution> fetchNodeExecutionsByNotifyId(String planExecutionId, String parentId);

  List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, Status status);

  List<NodeExecution> fetchNodeExecutionsByStatuses(@NonNull String planExecutionId, EnumSet<Status> statuses);

  NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<Update> ops);

  NodeExecution updateStatusWithOps(
      @NonNull String nodeExecutionId, @NonNull Status targetStatus, Consumer<Update> ops);

  default NodeExecution updateStatus(@NonNull String nodeExecutionId, @NonNull Status targetStatus) {
    return updateStatusWithOps(nodeExecutionId, targetStatus, null);
  }

  NodeExecution save(NodeExecution nodeExecution);

  List<NodeExecution> fetchChildrenNodeExecutionsByStatuses(
      String planExecutionId, List<String> parentIds, EnumSet<Status> statuses);

  boolean markLeavesDiscontinuingOnAbort(
      String interruptId, ExecutionInterruptType interruptType, String planExecutionId, List<String> leafInstanceIds);

  boolean markRetried(String nodeExecutionId);

  boolean updateRelationShipsForRetryNode(String nodeExecutionId, String newNodeExecutionId);

  Optional<NodeExecution> getByNodeIdentifier(@NonNull String nodeIdentifier, @NonNull String planExecutionId);

  List<NodeExecution> findByParentIdAndStatusIn(String parentId, EnumSet<Status> flowingStatuses);

  StepParameters extractStepParameters(NodeExecution nodeExecution);

  StepParameters extractResolvedStepParameters(NodeExecution nodeExecution);
}
