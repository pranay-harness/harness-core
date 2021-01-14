package io.harness.states;

import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.CALLBACK_IDS;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CiStepOutcome;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
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
        ambiance, RefObjectUtils.getSweepingOutputRefObject(CALLBACK_IDS));
    String stepIdentifier = AmbianceUtils.obtainStepIdentifier(ambiance);
    log.info("Waiting on response for task id {} and step Id {}", stepTaskDetails.getTaskIds().get(stepIdentifier),
        stepIdentifier);
    return AsyncExecutableResponse.newBuilder()
        .addCallbackIds(stepTaskDetails.getTaskIds().get(stepIdentifier))
        .build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, CIStepInfo stepParameters, Map<String, ResponseData> responseDataMap) {
    String stepIdentifier = AmbianceUtils.obtainStepIdentifier(ambiance);
    StepStatusTaskResponseData stepStatusTaskResponseData =
        (StepStatusTaskResponseData) responseDataMap.values().iterator().next();
    StepStatus stepStatus = stepStatusTaskResponseData.getStepStatus();

    log.info("Received response {} for step {}", stepStatus.getStepExecutionStatus(), stepIdentifier);
    if (stepStatus.getStepExecutionStatus() == StepExecutionStatus.SUCCESS) {
      StepResponse.StepOutcome stepOutcome = null;
      if (stepStatus.getOutput() != null) {
        stepOutcome =
            StepResponse.StepOutcome.builder()
                .outcome(
                    CiStepOutcome.builder().outputVariables(((StepMapOutput) stepStatus.getOutput()).getMap()).build())
                .name("output")
                .build();
      }
      return StepResponse.builder().status(Status.SUCCEEDED).stepOutcome(stepOutcome).build();
    } else if (stepStatus.getStepExecutionStatus() == StepExecutionStatus.SKIPPED) {
      return StepResponse.builder().status(Status.SKIPPED).build();
    } else {
      return StepResponse.builder()
          .status(Status.FAILED)
          .failureInfo(FailureInfo.newBuilder()
                           .setErrorMessage(stepStatus.getError())
                           .addAllFailureTypes(EnumSet.of(FailureType.APPLICATION_FAILURE))
                           .build())
          .build();
    }
  }

  @Override
  public void handleAbort(Ambiance ambiance, CIStepInfo stateParameters, AsyncExecutableResponse executableResponse) {}
}
