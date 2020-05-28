package io.harness.engine.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.state.io.StepParameters;
import lombok.NonNull;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

@OwnedBy(CDC)
public interface NodeExecutionService {
  List<NodeExecution> fetchNodeExecutions(String planExecutionId);

  NodeExecution get(String nodeExecutionId);

  void updateResolvedStepParameters(String nodeExecutionId, StepParameters stepParameters);

  List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, NodeExecutionStatus status);

  List<NodeExecution> fetchNodeExecutionsByStatuses(
      @NonNull String planExecutionId, EnumSet<NodeExecutionStatus> statuses);

  NodeExecution update(@NonNull String nodeExecutionId, @NonNull Consumer<UpdateOperations<NodeExecution>> ops);
}
