package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.retry.RetryStageInfo;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.NonNull;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PIPELINE)
public interface NodeExecutionService {
  NodeExecution get(String nodeExecutionId);

  NodeExecution getByPlanNodeUuid(String planNodeUuid, String planExecutionId);

  List<NodeExecution> fetchNodeExecutions(String planExecutionId);

  List<NodeExecution> fetchNodeExecutionsWithoutOldRetries(String planExecutionId);

  List<NodeExecution> fetchNodeExecutionsWithoutOldRetriesAndStatusIn(String planExecutionId, EnumSet<Status> statuses);

  List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId);

  List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, Status status);

  NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<Update> ops);

  NodeExecution updateStatusWithOps(@NonNull String nodeExecutionId, @NonNull Status targetStatus, Consumer<Update> ops,
      EnumSet<Status> overrideStatusSet);

  NodeExecution save(NodeExecution nodeExecution);

  NodeExecution save(NodeExecutionProto nodeExecution);

  long markLeavesDiscontinuing(String planExecutionId, List<String> leafInstanceIds);

  long markAllLeavesDiscontinuing(String planExecutionId, EnumSet<Status> statuses);

  List<NodeExecution> findAllNodeExecutionsTrimmed(String planExecutionId);

  boolean markRetried(String nodeExecutionId);

  boolean updateRelationShipsForRetryNode(String nodeExecutionId, String newNodeExecutionId);

  Optional<NodeExecution> getByNodeIdentifier(@NonNull String nodeIdentifier, @NonNull String planExecutionId);

  List<NodeExecution> findByParentIdAndStatusIn(String parentId, EnumSet<Status> flowingStatuses);

  default List<NodeExecution> findAllChildren(String planExecutionId, String parentId, boolean includeParent) {
    return findAllChildrenWithStatusIn(planExecutionId, parentId, EnumSet.noneOf(Status.class), includeParent);
  }

  List<NodeExecution> findAllChildrenWithStatusIn(
      String planExecutionId, String parentId, EnumSet<Status> flowingStatuses, boolean includeParent);

  List<NodeExecution> fetchNodeExecutionsByParentId(String nodeExecutionId, boolean oldRetry);

  boolean errorOutActiveNodes(String planExecutionId);

  boolean removeTimeoutInstances(String nodeExecutionId);

  List<RetryStageInfo> getStageDetailFromPlanExecutionId(String planExecutionId);

  Map<String, String> fetchNodeExecutionUuidFromNodeUuidsAndPlanExecutionId(
      List<String> uuidForSkipNode, String previousExecutionId);
}
