package io.harness.pms.plan.execution;

import io.harness.plan.Plan;
import io.harness.pms.plan.PlanCreationBlobResponse;
import io.harness.pms.plan.PlanNodeProto;

import java.util.Collection;

public class PlanExecutionUtils {
  public static Plan extractPlan(PlanCreationBlobResponse planCreationBlobResponse) {
    Plan.PlanBuilder planBuilder = Plan.builder();
    Collection<PlanNodeProto> planNodeProtoList = planCreationBlobResponse.getNodesMap().values();
    for (PlanNodeProto planNodeProto : planNodeProtoList) {
      planBuilder.node(planNodeProto);
    }
    return planBuilder.build();
  }
}
