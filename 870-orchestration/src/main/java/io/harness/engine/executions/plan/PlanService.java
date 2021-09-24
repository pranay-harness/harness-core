package io.harness.engine.executions.plan;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;

import java.util.Optional;

@OwnedBy(PIPELINE)
public interface PlanService {
  Plan save(Plan plan);

  PlanNode fetchNode(String planId, String nodeId);

  Plan fetchPlan(String planId);

  Optional<Plan> fetchPlanOptional(String planId);
}
