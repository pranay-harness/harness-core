package io.harness.pms.mongo;

import io.harness.pms.execution.NodeExecution;

public class NodeExecutionReadConverter extends ProtoReadConverter<NodeExecution> {
  public NodeExecutionReadConverter() {
    super(NodeExecution.class);
  }
}
