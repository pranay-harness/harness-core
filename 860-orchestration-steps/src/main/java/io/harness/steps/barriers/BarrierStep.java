package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.barrier.Barrier.State.DOWN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.springdata.HMongoTemplate;
import io.harness.steps.StepSpecTypeConstants;
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
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(StepSpecTypeConstants.BARRIER).build();

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
    BarrierExecutionInstance barrierExecutionInstance = barrierService.findByPlanNodeIdAndPlanExecutionId(
        AmbianceUtils.obtainCurrentSetupId(ambiance), ambiance.getPlanExecutionId());
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
    BarrierExecutionInstance barrierExecutionInstance = barrierService.findByPlanNodeIdAndPlanExecutionId(
        AmbianceUtils.obtainCurrentSetupId(ambiance), ambiance.getPlanExecutionId());

    log.info(
        "Barrier Step getting executed. RuntimeId: [{}], barrierUuid [{}], barrierIdentifier [{}], barrierGroupId [{}]",
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), barrierExecutionInstance.getUuid(),
        barrierStepParameters.getIdentifier(), barrierExecutionInstance.getBarrierGroupId());

    barrierService.update(barrierExecutionInstance);

    return AsyncExecutableResponse.newBuilder().addCallbackIds(barrierExecutionInstance.getBarrierGroupId()).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, BarrierStepParameters barrierStepParameters, Map<String, ResponseData> responseDataMap) {
    // if barrier is still in STANDING => update barrier state
    BarrierExecutionInstance barrierExecutionInstance =
        updateBarrierExecutionInstance(AmbianceUtils.obtainCurrentSetupId(ambiance), ambiance.getPlanExecutionId());

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    BarrierResponseData responseData =
        (BarrierResponseData) responseDataMap.get(barrierExecutionInstance.getBarrierGroupId());
    if (responseData.isFailed()) {
      stepResponseBuilder.status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder().setErrorMessage(responseData.getErrorMessage()).build());
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
      Ambiance ambiance, BarrierStepParameters stepParameters, AsyncExecutableResponse executableResponse) {
    updateBarrierExecutionInstance(AmbianceUtils.obtainCurrentSetupId(ambiance), ambiance.getPlanExecutionId());
  }

  private BarrierExecutionInstance updateBarrierExecutionInstance(String planNodeId, String planExecutionId) {
    BarrierExecutionInstance barrierExecutionInstance =
        barrierService.findByPlanNodeIdAndPlanExecutionId(planNodeId, planExecutionId);
    return barrierService.update(barrierExecutionInstance);
  }
}
