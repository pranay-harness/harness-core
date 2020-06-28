package io.harness.state.core.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Redesign
public class DummyStep implements Step, SyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("DUMMY").build();

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    logger.info("Dummy Step getting executed. Identifier: {}", ambiance.obtainCurrentLevel().getIdentifier());
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
