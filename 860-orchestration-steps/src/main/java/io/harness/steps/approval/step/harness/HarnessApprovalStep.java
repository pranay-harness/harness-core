package io.harness.steps.approval.step.harness;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;

import java.util.Map;

public class HarnessApprovalStep
    implements AsyncExecutable<io.harness.steps.approval.step.harness.HarnessApprovalStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.HARNESS_APPROVAL).build();

  @Override
  public Class<io.harness.steps.approval.step.harness.HarnessApprovalStepParameters> getStepParametersClass() {
    return io.harness.steps.approval.step.harness.HarnessApprovalStepParameters.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(Ambiance ambiance,
      io.harness.steps.approval.step.harness.HarnessApprovalStepParameters stepParameters,
      StepInputPackage inputPackage) {
    return AsyncExecutableResponse.newBuilder().build();
  }

  @Override
  public StepResponse handleAsyncResponse(Ambiance ambiance,
      io.harness.steps.approval.step.harness.HarnessApprovalStepParameters stepParameters,
      Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, HarnessApprovalStepParameters stepParameters, AsyncExecutableResponse executableResponse) {}
}
