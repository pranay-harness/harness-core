package io.harness.pms.sdk.core.facilitator.async;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.facilitator.FacilitatorUtils;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.inject.Inject;
import java.time.Duration;

@OwnedBy(CDC)
public class AsyncFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.ASYNC).build();

  @Inject private FacilitatorUtils facilitatorUtils;

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, byte[] parameters, StepInputPackage inputPackage) {
    Duration waitDuration = facilitatorUtils.extractWaitDurationFromDefaultParams(parameters);
    return FacilitatorResponse.builder().executionMode(ExecutionMode.ASYNC).initialWait(waitDuration).build();
  }
}
