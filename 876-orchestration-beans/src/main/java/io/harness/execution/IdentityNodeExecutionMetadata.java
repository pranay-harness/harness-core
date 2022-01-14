package io.harness.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plan.NodeType;

@OwnedBy(HarnessTeam.PIPELINE)
public class IdentityNodeExecutionMetadata implements PmsNodeExecutionMetadata {
  @Override
  public NodeType forNodeType() {
    return NodeType.IDENTITY_PLAN_NODE;
  }
}
