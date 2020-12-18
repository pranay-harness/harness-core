package io.harness.engine;

import io.harness.pms.contracts.execution.NodeExecutionProto;

public interface StepTypeLookupService {
  String findNodeExecutionServiceName(NodeExecutionProto nodeExecution);
}
