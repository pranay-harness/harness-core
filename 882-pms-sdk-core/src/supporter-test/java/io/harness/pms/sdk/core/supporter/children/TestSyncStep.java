package io.harness.pms.sdk.core.supporter.children;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.supporter.async.TestStepParameters;

@OwnedBy(HarnessTeam.PIPELINE)
public class TestSyncStep implements SyncExecutable<TestStepParameters> {
  @Override
  public Class<TestStepParameters> getStepParametersClass() {
    return TestStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, TestStepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
