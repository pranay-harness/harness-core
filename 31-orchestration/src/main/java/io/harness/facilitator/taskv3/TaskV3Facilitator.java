package io.harness.facilitator.taskv3;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;

@OwnedBy(CDC)
@Redesign
public class TaskV3Facilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK_V3).build();

  @Override
  public FacilitatorResponse facilitate(Ambiance ambiance, StepParameters stepParameters,
      FacilitatorParameters parameters, StepInputPackage inputPackage) {
    return FacilitatorResponse.builder()
        .executionMode(ExecutionMode.TASK_V3)
        .initialWait(parameters.getWaitDurationSeconds())
        .build();
  }
}
