package io.harness.engine.pms.execution.strategy;

import io.harness.engine.pms.execution.strategy.identity.IdentityNodeExecutionStrategy;
import io.harness.engine.pms.execution.strategy.plan.PlanExecutionStrategy;
import io.harness.engine.pms.execution.strategy.plannode.PlanNodeExecutionStrategy;
import io.harness.plan.NodeType;

import com.google.inject.Inject;

public class NodeExecutionStrategyFactory {
  @Inject private PlanNodeExecutionStrategy planNodeExecutionStrategy;
  @Inject private PlanExecutionStrategy planExecutionStrategy;
  @Inject private IdentityNodeExecutionStrategy identityNodeExecutionStrategy;

  @SuppressWarnings("rawtypes")
  public NodeExecutionStrategy obtainStrategy(NodeType nodeType) {
    switch (nodeType) {
      case PLAN:
        return planExecutionStrategy;
      case PLAN_NODE:
        return planNodeExecutionStrategy;
      case IDENTITY_PLAN_NODE:
        return identityNodeExecutionStrategy;
      default:
        throw new UnsupportedOperationException("No strategy present for Node Type" + nodeType.toString());
    }
  }
}
