package io.harness.states;

import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.CALLBACK_IDS;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CiStepOutcome;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.FailureType;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.refObjects.RefObjectUtil;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStepExecutable implements AsyncExecutable<CIStepInfo> {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<CIStepInfo> getStepParametersClass() {
    return CIStepInfo.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, CIStepInfo stepParameters, StepInputPackage inputPackage) {
    StepTaskDetails stepTaskDetails = (StepTaskDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtil.getSweepingOutputRefObject(CALLBACK_IDS));

    log.info("Waiting on response for task id {} and step Id {}",
        stepTaskDetails.getTaskIds().get(stepParameters.getIdentifier()), stepParameters.getIdentifier());
    return AsyncExecutableResponse.builder()
        .callbackId(stepTaskDetails.getTaskIds().get(stepParameters.getIdentifier()))
        .build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, CIStepInfo stepParameters, Map<String, ResponseData> responseDataMap) {
    StepStatusTaskResponseData stepStatusTaskResponseData =
        (StepStatusTaskResponseData) responseDataMap.values().iterator().next();
    StepStatus stepStatus = stepStatusTaskResponseData.getStepStatus();

    log.info("Received response {} for step {}", stepStatus.getStepExecutionStatus(), stepParameters.getIdentifier());
    if (stepStatus.getStepExecutionStatus() == StepExecutionStatus.SUCCESS) {
      return StepResponse.builder()
          .status(Status.SUCCEEDED)
          .stepOutcome(StepResponse.StepOutcome.builder()
                           .outcome(CiStepOutcome.builder().output(stepStatus.getOutput()).build())
                           .name(stepParameters.getIdentifier())
                           .build())
          .build();
    } else {
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.builder()
                           .errorMessage(stepStatus.getError())
                           .failureTypes(EnumSet.of(FailureType.APPLICATION_ERROR))
                           .build())
          .build();
    }
  }

  @Override
  public void handleAbort(Ambiance ambiance, CIStepInfo stateParameters, AsyncExecutableResponse executableResponse) {}
}
