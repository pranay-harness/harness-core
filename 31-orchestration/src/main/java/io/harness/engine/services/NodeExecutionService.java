package io.harness.engine.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.state.io.StepParameters;

import java.util.EnumSet;
import java.util.List;

@OwnedBy(CDC)
public interface NodeExecutionService {
  List<NodeExecution> fetchNodeExecutions(String planExecutionId);
  void updateResolvedStepParameters(String nodeExecutionId, StepParameters stepParameters);

  List<NodeExecution> fetchNodeExecutionsByStatus(String planExecutionId, NodeExecutionStatus status);

  List<NodeExecution> fetchNodeExecutionsByStatuses(String planExecutionId, EnumSet<NodeExecutionStatus> statuses);
}
