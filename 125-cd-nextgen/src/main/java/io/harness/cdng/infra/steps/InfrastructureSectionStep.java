package io.harness.cdng.infra.steps;

import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.EnvironmentMapper;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.tags.TagUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class InfrastructureSectionStep implements ChildExecutable<InfraSectionStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.INFRASTRUCTURE_SECTION.getName()).build();

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private EnvironmentService environmentService;

  @Override
  public Class<InfraSectionStepParameters> getStepParametersClass() {
    return InfraSectionStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, InfraSectionStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for InfraSection Step [{}]", stepParameters);
    EnvironmentOutcome environmentOutcome = processEnvironment(ambiance, stepParameters);
    executionSweepingOutputResolver.consume(
        ambiance, OutcomeExpressionConstants.ENVIRONMENT, environmentOutcome, StepOutcomeGroup.STAGE.name());

    return ChildExecutableResponse.newBuilder().setChildNodeId(stepParameters.getChildNodeID()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, InfraSectionStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed execution for InfraSection Step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  EnvironmentOutcome processEnvironment(Ambiance ambiance, InfraSectionStepParameters pipelineInfrastructure) {
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
      InfraSectionStepParameters pipelineInfrastructure, EnvironmentYaml environmentOverrides, Ambiance ambiance) {
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

    TagUtils.removeUuidFromTags(environmentYaml.getTags());

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

  private EnvironmentYaml createEnvYamlFromEnvRef(
      InfraSectionStepParameters pipelineInfrastructure, Ambiance ambiance) {
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
