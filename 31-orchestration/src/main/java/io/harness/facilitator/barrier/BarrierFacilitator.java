package io.harness.facilitator.barrier;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.barriers.BarrierExecutionInstance;
import io.harness.engine.barriers.BarrierService;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.FacilitatorResponse.FacilitatorResponseBuilder;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.state.core.barrier.BarrierStepParameters;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;

import java.util.List;

public class BarrierFacilitator implements Facilitator {
  public static final FacilitatorType FACILITATOR_TYPE =
      FacilitatorType.builder().type(FacilitatorType.BARRIER).build();

  @Inject private BarrierService barrierService;

  @Override
  public FacilitatorResponse facilitate(Ambiance ambiance, StepParameters stepParameters,
      FacilitatorParameters parameters, StepInputPackage inputPackage) {
    FacilitatorResponseBuilder responseBuilder =
        FacilitatorResponse.builder().initialWait(parameters.getWaitDurationSeconds());
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
