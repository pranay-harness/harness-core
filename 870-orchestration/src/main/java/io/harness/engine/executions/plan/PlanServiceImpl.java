/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.executions.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.repositories.PlanRepository;

import com.google.inject.Inject;
import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
public class PlanServiceImpl implements PlanService {
  @Inject private PlanRepository planRepository;

  @Override
  public Plan fetchPlan(String planId) {
    Optional<Plan> planOptional = planRepository.findById(planId);
    if (!planOptional.isPresent()) {
      throw new InvalidRequestException("Plan not found for id" + planId);
    }
    return planOptional.get();
  }

  @Override
  public Optional<Plan> fetchPlanOptional(String planId) {
    return planRepository.findById(planId);
  }
  @Override
  public Plan save(Plan plan) {
    return planRepository.save(plan);
  }

  @Override
  public PlanNodeProto fetchNode(String planId, String nodeId) {
    return fetchPlan(planId).fetchNode(nodeId);
  }
}
