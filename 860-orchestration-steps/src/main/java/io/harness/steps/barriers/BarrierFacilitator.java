package io.harness.steps.barriers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse.FacilitatorResponseBuilder;
import io.harness.pms.sdk.core.facilitator.FacilitatorUtils;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.service.BarrierService;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;

public class BarrierFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.BARRIER).build();

  @Inject private BarrierService barrierService;
  @Inject private FacilitatorUtils facilitatorUtils;

  @Override
  public FacilitatorResponse facilitate(
      Ambiance ambiance, StepParameters stepParameters, byte[] parameters, StepInputPackage inputPackage) {
    Duration waitDuration = facilitatorUtils.extractWaitDurationFromDefaultParams(parameters);
    FacilitatorResponseBuilder responseBuilder = FacilitatorResponse.builder().initialWait(waitDuration);
    if (isSingleBarrier(((BarrierStepParameters) stepParameters).getIdentifier(), ambiance.getPlanExecutionId())) {
      responseBuilder.executionMode(ExecutionMode.SYNC);
    } else {
      responseBuilder.executionMode(ExecutionMode.ASYNC);
    }
    return responseBuilder.build();
  }

  private boolean isSingleBarrier(String identifier, String planExecutionId) {
    List<BarrierExecutionInstance> barrierExecutionInstances =
        barrierService.findByIdentifierAndPlanExecutionId(identifier, planExecutionId);
    return isEmpty(barrierExecutionInstances) || barrierExecutionInstances.size() == 1;
  }
}
