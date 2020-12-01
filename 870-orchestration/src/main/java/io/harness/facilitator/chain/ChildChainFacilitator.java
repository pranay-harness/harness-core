package io.harness.facilitator.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.FacilitatorUtils;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.state.io.StepInputPackage;

import com.google.inject.Inject;
import java.time.Duration;

@OwnedBy(CDC)
public class ChildChainFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build();

  @Inject private FacilitatorUtils facilitatorUtils;

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, byte[] parameters, StepInputPackage inputPackage) {
    Duration waitDuration = facilitatorUtils.extractWaitDurationFromDefaultParams(parameters);
    return FacilitatorResponse.builder().executionMode(ExecutionMode.CHILD_CHAIN).initialWait(waitDuration).build();
  }
}
