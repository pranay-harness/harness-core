package io.harness.plancreator.steps.common.rollback;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(CDC)
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
public abstract class TaskExecutableWithRollback<R extends ResponseData>
    implements TaskExecutable<StepElementParameters, R> {
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public void handleFailureInterrupt(
      Ambiance ambiance, StepElementParameters stepParameters, Map<String, String> metadata) {
    RollbackExecutableUtility.publishRollbackInfo(ambiance, stepParameters, metadata, executionSweepingOutputService);
  }
}
