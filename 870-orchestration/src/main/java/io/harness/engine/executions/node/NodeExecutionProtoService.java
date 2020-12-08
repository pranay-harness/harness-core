package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.steps.io.StepParameters;

@OwnedBy(CDC)
public interface NodeExecutionProtoService {
  NodeExecution save(NodeExecutionProto nodeExecution);

  StepParameters extractStepParameters(NodeExecutionProto nodeExecution);

  StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution);
}
