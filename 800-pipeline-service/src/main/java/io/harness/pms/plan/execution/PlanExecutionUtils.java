package io.harness.pms.plan.execution;

import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanNodeProto;

import java.util.Collection;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanExecutionUtils {
  public Plan extractPlan(PlanCreationBlobResponse planCreationBlobResponse) {
    Plan.PlanBuilder planBuilder = Plan.builder();
    Collection<PlanNodeProto> planNodeProtoList = planCreationBlobResponse.getNodesMap().values();
    for (PlanNodeProto planNodeProto : planNodeProtoList) {
      planBuilder.node(planNodeProto);
    }
    if (planCreationBlobResponse.getStartingNodeId() != null) {
      planBuilder.startingNodeId(planCreationBlobResponse.getStartingNodeId());
    }
    return planBuilder.build();
  }
}
