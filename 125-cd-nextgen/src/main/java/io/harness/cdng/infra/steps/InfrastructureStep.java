package io.harness.cdng.infra.steps;

import static io.harness.ng.core.mapper.TagMapper.convertToList;

import io.harness.cdng.environment.EnvironmentMapper;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureMapper;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepOutcomeGroup;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.Optional;

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

    EnvironmentOutcome environmentOutcome = processEnvironment(ambiance, pipelineInfrastructure);

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
    InfrastructureOutcome infrastructureOutcome = InfrastructureMapper.toOutcome(finalInfrastructure);

    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder()
                         .outcome(infrastructureOutcome)
                         .name(OutcomeExpressionConstants.INFRASTRUCTURE)
                         .group(StepOutcomeGroup.STAGE.name())
                         .build())
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.ENVIRONMENT)
                         .group(StepOutcomeGroup.STAGE.name())
                         .outcome(environmentOutcome)
                         .build())
        .build();
  }

  @VisibleForTesting
  EnvironmentOutcome processEnvironment(Ambiance ambiance, PipelineInfrastructure pipelineInfrastructure) {
    EnvironmentYaml environmentOverrides = null;

    if (pipelineInfrastructure.getUseFromStage() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides() != null) {
      environmentOverrides = pipelineInfrastructure.getUseFromStage().getOverrides().getEnvironment();
      if (EmptyPredicate.isEmpty(environmentOverrides.getName())) {
        environmentOverrides.setName(environmentOverrides.getIdentifier());
      }
    }
    return processEnvironment(pipelineInfrastructure, environmentOverrides, ambiance);
  }

  private EnvironmentOutcome processEnvironment(
      PipelineInfrastructure pipelineInfrastructure, EnvironmentYaml environmentOverrides, Ambiance ambiance) {
    EnvironmentYaml environmentYaml = pipelineInfrastructure.getEnvironment();
    if (environmentYaml == null) {
      environmentYaml = createEnvYamlFromEnvRef(pipelineInfrastructure, ambiance);
    }
    if (EmptyPredicate.isEmpty(environmentYaml.getName())) {
      environmentYaml.setName(environmentYaml.getIdentifier());
    }
    EnvironmentYaml finalEnvironmentYaml =
        environmentOverrides != null ? environmentYaml.applyOverrides(environmentOverrides) : environmentYaml;
    Environment environment = getEnvironmentObject(finalEnvironmentYaml, ambiance);
    environmentService.upsert(environment);
    return EnvironmentMapper.toOutcome(finalEnvironmentYaml);
  }

  private Environment getEnvironmentObject(EnvironmentYaml environmentYaml, Ambiance ambiance) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String projectIdentifier = AmbianceHelper.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);

    return Environment.builder()
        .name(environmentYaml.getName())
        .accountId(accountId)
        .type(environmentYaml.getType())
        .identifier(environmentYaml.getIdentifier())
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .tags(convertToList(environmentYaml.getTags()))
        .build();
  }

  private EnvironmentYaml createEnvYamlFromEnvRef(PipelineInfrastructure pipelineInfrastructure, Ambiance ambiance) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String projectIdentifier = AmbianceHelper.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);
    String envIdentifier = pipelineInfrastructure.getEnvironmentRef().getValue();

    Optional<Environment> optionalEnvironment =
        environmentService.get(accountId, orgIdentifier, projectIdentifier, envIdentifier, false);
    if (optionalEnvironment.isPresent()) {
      Environment env = optionalEnvironment.get();
      return EnvironmentYaml.builder()
          .identifier(envIdentifier)
          .name(env.getName())
          .description(env.getDescription() == null ? null : ParameterField.createValueField(env.getDescription()))
          .type(env.getType())
          .tags(TagMapper.convertToMap(env.getTags()))
          .build();
    }
    throw new InvalidRequestException("Env with identifier " + envIdentifier + " does not exist");
  }
}
