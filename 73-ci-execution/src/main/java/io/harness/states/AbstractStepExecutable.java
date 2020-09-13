package io.harness.states;

import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.CALLBACK_IDS;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.async.AsyncExecutable;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.managerclient.ManagerCIResource;
import io.harness.references.SweepingOutputRefObject;
import io.harness.state.Step;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractStepExecutable implements Step, AsyncExecutable<CIStepInfo> {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ManagerCIResource managerCIResource;
  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, CIStepInfo stepParameters, StepInputPackage inputPackage) {
    StepTaskDetails stepTaskDetails = (StepTaskDetails) executionSweepingOutputResolver.resolve(
        ambiance, SweepingOutputRefObject.builder().name(CALLBACK_IDS).build());

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

    if (stepStatus.getStepExecutionStatus() == StepExecutionStatus.SUCCESS) {
      return StepResponse.builder()
          .status(Status.SUCCEEDED)
          .stepOutcome(StepResponse.StepOutcome.builder().build())
          .build();
    } else {
      return StepResponse.builder().status(Status.FAILED).build();
    }
  }

  @Override
  public void handleAbort(Ambiance ambiance, CIStepInfo stateParameters, AsyncExecutableResponse executableResponse) {}

  protected abstract List<String> getExecCommand(CIStepInfo ciStepInfo);
}
