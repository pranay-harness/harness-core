package io.harness.steps.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.AmbianceUtils;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.steps.OrchestrationStepTypes;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Redesign
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
