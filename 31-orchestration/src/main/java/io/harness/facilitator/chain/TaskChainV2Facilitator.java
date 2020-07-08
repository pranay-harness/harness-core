package io.harness.facilitator.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;

@OwnedBy(CDC)
@Redesign
public class TaskChainV2Facilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.builder().type(FacilitatorType.TASK_CHAIN_V2).build();

  @Override
  public FacilitatorResponse facilitate(Ambiance ambiance, StepParameters stepParameters,
      FacilitatorParameters parameters, StepInputPackage inputPackage) {
    return FacilitatorResponse.builder()
        .executionMode(ExecutionMode.TASK_CHAIN_V2)
        .initialWait(parameters.getWaitDurationSeconds())
        .build();
  }
}
