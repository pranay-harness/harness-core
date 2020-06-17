package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import lombok.NonNull;
import org.springframework.data.mongodb.core.query.Update;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

@OwnedBy(CDC)
public interface NodeExecutionService {
  NodeExecution get(String nodeExecutionId);

  List<NodeExecution> fetchNodeExecutions(String planExecutionId);

  List<NodeExecution> fetchNodeExecutionsWithoutOldRetries(String planExecutionId);

  List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId);

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
}
