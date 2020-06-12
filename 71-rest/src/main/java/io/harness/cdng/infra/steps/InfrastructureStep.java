package io.harness.cdng.infra.steps;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.execution.status.Status;
import io.harness.executionplan.plancreator.beans.StepGroup;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepTransput;

import java.util.List;

public class InfrastructureStep implements Step, SyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("INFRASTRUCTURE").build();

  InfraMapping createInfraMappingObject(String serviceIdentifier, Infrastructure infrastructureSpec) {
    InfraMapping infraMapping = infrastructureSpec.getInfraMapping();
    infraMapping.setServiceIdentifier(serviceIdentifier);
    return infraMapping;
  }

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    Infrastructure infrastructure = (Infrastructure) stepParameters;
    // TODO: render variables later
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(
            StepOutcome.builder().outcome(infrastructure).name("infrastructure").group(StepGroup.STAGE.name()).build())
        .build();
  }
}
