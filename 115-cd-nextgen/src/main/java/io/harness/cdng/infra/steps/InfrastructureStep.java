package io.harness.cdng.infra.steps;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.execution.status.Status;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;

public class InfrastructureStep implements Step, SyncExecutable<InfraStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("INFRASTRUCTURE").build();

  InfraMapping createInfraMappingObject(String serviceIdentifier, Infrastructure infrastructureSpec) {
    InfraMapping infraMapping = infrastructureSpec.getInfraMapping();
    infraMapping.setServiceIdentifier(serviceIdentifier);
    return infraMapping;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, InfraStepParameters infraStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    Infrastructure infrastructure = infraStepParameters.getInfrastructureOverrides() != null
        ? infraStepParameters.getInfrastructure().applyOverrides(infraStepParameters.getInfrastructureOverrides())
        : infraStepParameters.getInfrastructure();
    // TODO: render variables later
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder()
                         .outcome(infrastructure)
                         .name(OutcomeExpressionConstants.INFRASTRUCTURE.getName())
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .build();
  }
}
