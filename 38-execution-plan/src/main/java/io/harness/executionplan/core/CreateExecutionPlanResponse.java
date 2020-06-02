package io.harness.executionplan.core;

import io.harness.executionplan.core.impl.CreateExecutionPlanResponseImpl;
import io.harness.executionplan.core.impl.CreateExecutionPlanResponseImpl.CreateExecutionPlanResponseImplBuilder;
import io.harness.plan.PlanNode;

import java.util.List;

public interface CreateExecutionPlanResponse {
  List<PlanNode> getPlanNodes();
  String getStartingNodeId();

  static CreateExecutionPlanResponseImplBuilder builder() {
    return CreateExecutionPlanResponseImpl.builder();
  }
}
