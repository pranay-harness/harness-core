/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.executions.plan;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.PlanNodeProto;

import java.util.Optional;

@OwnedBy(PIPELINE)
public interface PlanService {
  Plan save(Plan plan);

  PlanNodeProto fetchNode(String planId, String nodeId);

  Plan fetchPlan(String planId);

  Optional<Plan> fetchPlanOptional(String planId);
}
