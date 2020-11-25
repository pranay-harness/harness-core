package io.harness.steps.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.Step;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.steps.OrchestrationStepTypes;

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
    log.info("Dummy Step getting executed. Identifier: {}", ambiance.obtainCurrentLevel().getIdentifier());
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
