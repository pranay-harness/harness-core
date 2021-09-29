package io.harness.steps.executable;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rollback.RollbackUtility;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class ChildExecutableWithRollbackAndRbac<T extends StepParameters>
    implements ChildExecutableWithRbac<T> {
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, T stepParameters, Map<String, ResponseData> responseDataMap) {
    RollbackUtility.publishRollbackInformation(ambiance, responseDataMap, executionSweepingOutputService);
    return handleChildResponseInternal(ambiance, stepParameters, responseDataMap);
  }

  public abstract StepResponse handleChildResponseInternal(
      Ambiance ambiance, T stepParameters, Map<String, ResponseData> responseDataMap);
}
