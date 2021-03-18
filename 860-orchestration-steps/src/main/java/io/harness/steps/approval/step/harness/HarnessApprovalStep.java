package io.harness.steps.approval.step.harness;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;

import java.util.Map;

public class HarnessApprovalStep
    implements SyncExecutable<HarnessApprovalStepParameters>, AsyncExecutable<HarnessApprovalStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(OrchestrationStepTypes.HARNESS_APPROVAL).build();

  @Override
  public Class<HarnessApprovalStepParameters> getStepParametersClass() {
    return HarnessApprovalStepParameters.class;
  }

  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, HarnessApprovalStepParameters stepParameters, StepInputPackage inputPackage) {
    return AsyncExecutableResponse.newBuilder().build();
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, HarnessApprovalStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, HarnessApprovalStepParameters stepParameters, AsyncExecutableResponse executableResponse) {}

  @Override
  public StepResponse executeSync(Ambiance ambiance, HarnessApprovalStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
