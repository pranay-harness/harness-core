package io.harness.steps.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.OrchestrationStepTypes;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class DummyStep implements SyncExecutable<DummyStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.DUMMY).build();

  @Override
  public Class<DummyStepParameters> getStepParametersClass() {
    return DummyStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, DummyStepParameters dummyStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Dummy Step getting executed. Identifier: {}",
        Preconditions.checkNotNull(AmbianceUtils.obtainCurrentLevel(ambiance)).getIdentifier());
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
