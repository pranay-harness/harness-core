package io.harness.engine.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import lombok.NonNull;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

@OwnedBy(CDC)
public interface NodeExecutionService {
  NodeExecution get(String nodeExecutionId);

  List<NodeExecution> fetchNodeExecutions(String planExecutionId);

  List<NodeExecution> fetchChildrenNodeExecutions(String planExecutionId, String parentId);

  List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, Status status);

  List<NodeExecution> fetchNodeExecutionsByStatuses(@NonNull String planExecutionId, EnumSet<Status> statuses);

  NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<UpdateOperations<NodeExecution>> ops);
}
