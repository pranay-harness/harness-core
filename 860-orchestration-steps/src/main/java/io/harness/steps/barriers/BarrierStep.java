package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;
import io.harness.steps.barriers.beans.BarrierResponseData;
import io.harness.steps.barriers.service.BarrierService;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
public class BarrierStep implements AsyncExecutable<BarrierStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(StepSpecTypeConstants.BARRIER).build();

  private static final String BARRIER = "barrier";

  @Inject private BarrierService barrierService;

  @Override
  public Class<BarrierStepParameters> getStepParametersClass() {
    return BarrierStepParameters.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, BarrierStepParameters barrierStepParameters, StepInputPackage inputPackage) {
    BarrierExecutionInstance barrierExecutionInstance = barrierService.findByIdentifierAndPlanExecutionId(
        barrierStepParameters.getIdentifier(), ambiance.getPlanExecutionId());

    log.info("Barrier Step getting executed. RuntimeId: [{}], barrierUuid [{}], barrierIdentifier [{}]",
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), barrierExecutionInstance.getUuid(),
        barrierStepParameters.getIdentifier());

    return AsyncExecutableResponse.newBuilder().addCallbackIds(barrierExecutionInstance.getUuid()).build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, BarrierStepParameters barrierStepParameters, Map<String, ResponseData> responseDataMap) {
    // if barrier is still in STANDING => update barrier state
    BarrierExecutionInstance barrierExecutionInstance =
        updateBarrierExecutionInstance(barrierStepParameters.getIdentifier(), ambiance.getPlanExecutionId());

    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    BarrierResponseData responseData = (BarrierResponseData) responseDataMap.get(barrierExecutionInstance.getUuid());
    if (responseData.isFailed()) {
      BarrierResponseData.BarrierError barrierError = responseData.getBarrierError();
      if (barrierError.isTimedOut()) {
        stepResponseBuilder.status(Status.EXPIRED);
      } else {
        stepResponseBuilder.status(Status.FAILED);
      }
      stepResponseBuilder.failureInfo(FailureInfo.newBuilder().setErrorMessage(barrierError.getErrorMessage()).build());
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
    updateBarrierExecutionInstance(stepParameters.getIdentifier(), ambiance.getPlanExecutionId());
  }

  private BarrierExecutionInstance updateBarrierExecutionInstance(String identifier, String planExecutionId) {
    BarrierExecutionInstance barrierExecutionInstance =
        barrierService.findByIdentifierAndPlanExecutionId(identifier, planExecutionId);
    return barrierService.update(barrierExecutionInstance);
  }
}
