package io.harness.engine.executions.plan;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plan.Node;
import io.harness.plan.Plan;

import java.util.Optional;

@OwnedBy(PIPELINE)
public interface PlanService {
  Plan save(Plan plan);

  <T extends Node> T fetchNode(String planId, String nodeId);

  Plan fetchPlan(String planId);

  Optional<Plan> fetchPlanOptional(String planId);
}
