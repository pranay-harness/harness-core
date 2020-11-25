package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.barrier.Barrier.State.DOWN;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.springdata.HMongoTemplate;
import io.harness.state.Step;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;
import io.harness.steps.barriers.beans.BarrierResponseData;
import io.harness.steps.barriers.service.BarrierService;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class BarrierStep implements SyncExecutable<BarrierStepParameters>, AsyncExecutable<BarrierStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.BARRIER).build();

  private static final String BARRIER = "barrier";

  @Inject private BarrierService barrierService;

  @Override
  public Class<BarrierStepParameters> getStepParametersClass() {
    return BarrierStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, BarrierStepParameters barrierStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    final String identifier = barrierStepParameters.getIdentifier();
    log.warn("There is only one barrier present for planExecution [{}] with [{}] identifier, passing through it...",
        ambiance.getPlanExecutionId(), identifier);
    BarrierExecutionInstance barrierExecutionInstance =
        barrierService.findByPlanNodeId(ambiance.obtainCurrentLevel().getSetupId());
    barrierExecutionInstance.setBarrierState(DOWN);
    HMongoTemplate.retry(() -> barrierService.save(barrierExecutionInstance));
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(
            StepResponse.StepOutcome.builder()
                .name(BARRIER)
                .outcome(BarrierOutcome.builder()
                             .message("There is only one barrier present with this identifier. Barrier went down")
                             .identifier(identifier)
                             .build())
                .build())
        .build();
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, BarrierStepParameters barrierStepParameters, StepInputPackage inputPackage) {
    BarrierExecutionInstance barrierExecutionInstance =
        barrierService.findByPlanNodeId(ambiance.obtainCurrentLevel().getSetupId());

    log.info(
        "Barrier Step getting executed. RuntimeId: [{}], barrierUuid [{}], barrierIdentifier [{}], barrierGroupId [{}]",
        ambiance.obtainCurrentLevel().getRuntimeId(), barrierExecutionInstance.getUuid(),
        barrierStepParameters.getIdentifier(), barrierExecutionInstance.getBarrierGroupId());

    barrierService.update(barrierExecutionInstance);

    return AsyncExecutableResponse.builder().callbackId(barrierExecutionInstance.getBarrierGroupId()).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, BarrierStepParameters barrierStepParameters, Map<String, ResponseData> responseDataMap) {
    // if barrier is still in STANDING => update barrier state
    BarrierExecutionInstance barrierExecutionInstance =
        updateBarrierExecutionInstance(ambiance.obtainCurrentLevel().getSetupId());

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    BarrierResponseData responseData =
        (BarrierResponseData) responseDataMap.get(barrierExecutionInstance.getBarrierGroupId());
    if (responseData.isFailed()) {
      stepResponseBuilder.status(Status.FAILED)
          .failureInfo(FailureInfo.builder().errorMessage(responseData.getErrorMessage()).build());
    } else {
      stepResponseBuilder.status(Status.SUCCEEDED);
    }

    return stepResponseBuilder
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(BARRIER)
                         .outcome(BarrierOutcome.builder().identifier(barrierExecutionInstance.getIdentifier()).build())
                         .build())
        .build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, BarrierStepParameters stateParameters, AsyncExecutableResponse executableResponse) {
    updateBarrierExecutionInstance(ambiance.obtainCurrentLevel().getSetupId());
  }

  private BarrierExecutionInstance updateBarrierExecutionInstance(String planNodeId) {
    BarrierExecutionInstance barrierExecutionInstance = barrierService.findByPlanNodeId(planNodeId);
    return barrierService.update(barrierExecutionInstance);
  }
}
