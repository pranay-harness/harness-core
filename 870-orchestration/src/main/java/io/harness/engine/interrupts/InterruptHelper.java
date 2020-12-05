package io.harness.engine.interrupts;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.execution.PlanExecution;
import io.harness.pms.ambiance.Ambiance;

import java.util.HashMap;

public class InterruptHelper {
  public static Ambiance buildFromPlanExecution(PlanExecution planExecution) {
    return Ambiance.newBuilder()
        .setPlanExecutionId(planExecution.getUuid())
        .putAllSetupAbstractions(
            isEmpty(planExecution.getSetupAbstractions()) ? new HashMap<>() : planExecution.getSetupAbstractions())
        .build();
  }
}
