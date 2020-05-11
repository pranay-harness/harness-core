package io.harness.engine.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;

import java.util.List;

@OwnedBy(CDC)
public interface NodeExecutionService {
  List<NodeExecution> fetchNodeExecutions(String planExecutionId);
}
