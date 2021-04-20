package io.harness.plancreator.steps.common.rollback;

import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;

import com.google.inject.Inject;
import java.util.Map;

public abstract class AsyncExecutableWithRollback implements AsyncExecutable<StepElementParameters> {
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;
  @Override
  public void handleFailure(Ambiance ambiance, StepElementParameters stepParameters, Map<String, String> metadata) {
    RollbackExecutableUtility.publishRollbackInfo(ambiance, stepParameters, metadata, executionSweepingOutputService);
  }
}
