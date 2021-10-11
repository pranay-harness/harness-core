package io.harness.execution;

import io.harness.plan.NodeType;

public class NodeExecutionMetadata implements PmsNodeExecutionMetadata {
  @Override
  public NodeType forNodeType() {
    return NodeType.PLAN_NODE;
  }
}
