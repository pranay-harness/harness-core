/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.executions.plan;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.plan.Node;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
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
  public Node fetchNode(String planId, String nodeId) {
    Plan plan = fetchPlan(planId);
    if (isNotEmpty(plan.getPlanNodes())) {
      return plan.fetchPlanNode(nodeId);
    }
    return PlanNode.fromPlanNodeProto(fetchPlan(planId).fetchNode(nodeId));
  }
}
