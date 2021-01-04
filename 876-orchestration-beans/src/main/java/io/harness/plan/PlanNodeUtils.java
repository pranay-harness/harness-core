package io.harness.plan;

import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.io.StepParameters;

public class PlanNodeUtils {
  public static PlanNodeProto cloneForRetry(PlanNodeProto planNodeProto, StepParameters stepParameters) {
    PlanNodeProto.Builder builder = PlanNodeProto.newBuilder(planNodeProto);
    builder.setStepParameters(stepParameters.toJson());
    return builder.build();
  }
}
