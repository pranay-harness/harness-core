package io.harness.plan;

import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

public class PlanNodeUtils {
  public static PlanNodeProto cloneForRetry(PlanNodeProto planNodeProto, StepParameters stepParameters) {
    PlanNodeProto.Builder builder = PlanNodeProto.newBuilder(planNodeProto);
    builder.setStepParameters(RecastOrchestrationUtils.toJson(stepParameters));
    return builder.build();
  }
}
