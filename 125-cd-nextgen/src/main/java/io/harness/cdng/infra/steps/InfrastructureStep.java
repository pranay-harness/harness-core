package io.harness.cdng.infra.steps;

import io.harness.cdng.infra.InfrastructureMapper;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;

public class InfrastructureStep implements SyncExecutable<InfraStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.INFRASTRUCTURE.getName()).build();

  @Inject private EnvironmentService environmentService;

  @Override
  public Class<InfraStepParameters> getStepParametersClass() {
    return InfraStepParameters.class;
  }

  InfraMapping createInfraMappingObject(String serviceIdentifier, Infrastructure infrastructureSpec) {
    InfraMapping infraMapping = infrastructureSpec.getInfraMapping();
    infraMapping.setServiceIdentifier(serviceIdentifier);
    return infraMapping;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, InfraStepParameters infraStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    PipelineInfrastructure pipelineInfrastructure = infraStepParameters.getPipelineInfrastructure();

    Infrastructure infraOverrides = null;
    if (pipelineInfrastructure.getUseFromStage() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides().getInfrastructureDefinition() != null) {
      infraOverrides =
          pipelineInfrastructure.getUseFromStage().getOverrides().getInfrastructureDefinition().getInfrastructure();
    }

    Infrastructure infrastructure = pipelineInfrastructure.getInfrastructureDefinition().getInfrastructure();
    Infrastructure finalInfrastructure =
        infraOverrides != null ? infrastructure.applyOverrides(infraOverrides) : infrastructure;
    if (finalInfrastructure == null) {
      throw new InvalidRequestException("Infrastructure definition can't be null or empty");
    }
    InfrastructureOutcome infrastructureOutcome = InfrastructureMapper.toOutcome(finalInfrastructure);

    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder()
                         .outcome(infrastructureOutcome)
                         .name(OutcomeExpressionConstants.INFRASTRUCTURE)
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .build();
  }
}
