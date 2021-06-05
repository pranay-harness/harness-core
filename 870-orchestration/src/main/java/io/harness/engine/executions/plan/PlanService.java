package io.harness.engine.executions.plan;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.PlanNodeProto;

@OwnedBy(PIPELINE)
public interface PlanService {
  PlanNodeProto fetchNode(String planId, String nodeId);

  Plan fetchPlan(String planId);
}
