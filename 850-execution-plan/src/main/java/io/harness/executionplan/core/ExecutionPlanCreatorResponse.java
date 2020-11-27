package io.harness.executionplan.core;

import io.harness.executionplan.core.impl.ExecutionPlanCreatorResponseImpl;
import io.harness.executionplan.core.impl.ExecutionPlanCreatorResponseImpl.ExecutionPlanCreatorResponseImplBuilder;
import io.harness.plan.PlanNode;

import java.util.List;

public interface ExecutionPlanCreatorResponse {
  List<PlanNode> getPlanNodes();
  String getStartingNodeId();

  static ExecutionPlanCreatorResponseImplBuilder builder() {
    return ExecutionPlanCreatorResponseImpl.builder();
  }
}
