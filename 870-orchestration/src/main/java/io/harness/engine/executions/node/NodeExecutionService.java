package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;

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

  List<NodeExecution> fetchNodeExecutionsWithoutOldRetriesAndStatusIn(String planExecutionId, EnumSet<Status> statuses);

  List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId);

  List<NodeExecution> fetchNodeExecutionsByNotifyId(String planExecutionId, String parentId, boolean isOldRetry);

  List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, Status status);

  List<NodeExecution> fetchNodeExecutionsByStatuses(@NonNull String planExecutionId, EnumSet<Status> statuses);

  NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<Update> ops);

  NodeExecution updateStatusWithOps(@NonNull String nodeExecutionId, @NonNull Status targetStatus, Consumer<Update> ops,
      EnumSet<Status> overrideStatusSet);

  NodeExecution save(NodeExecution nodeExecution);

  NodeExecution save(NodeExecutionProto nodeExecution);

  List<NodeExecution> fetchChildrenNodeExecutionsByStatuses(
      String planExecutionId, List<String> parentIds, EnumSet<Status> statuses);

  boolean markLeavesDiscontinuingOnAbort(
      String interruptId, InterruptType interruptType, String planExecutionId, List<String> leafInstanceIds);

  boolean markRetried(String nodeExecutionId);

  boolean updateRelationShipsForRetryNode(String nodeExecutionId, String newNodeExecutionId);

  Optional<NodeExecution> getByNodeIdentifier(@NonNull String nodeIdentifier, @NonNull String planExecutionId);

  List<NodeExecution> findByParentIdAndStatusIn(String parentId, EnumSet<Status> flowingStatuses);

  default List<NodeExecution> findAllChildren(String planExecutionId, String parentId, boolean includeParent) {
    return findAllChildrenWithStatusIn(planExecutionId, parentId, EnumSet.noneOf(Status.class), includeParent);
  }

  List<NodeExecution> findAllChildrenWithStatusIn(
      String planExecutionId, String parentId, EnumSet<Status> flowingStatuses, boolean includeParent);

  List<NodeExecution> fetchNodeExecutionsByStatusAndIdIn(String planExecutionId, Status status, List<String> targetIds);

  List<NodeExecution> fetchNodeExecutionsByParentId(String nodeExecutionId, boolean oldRetry);
}
