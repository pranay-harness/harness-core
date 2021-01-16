package io.harness.beans.stages;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure.Type;
import io.harness.beans.yaml.extended.infrastrucutre.UseFromStageInfraYaml;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("integrationStageStepParameters")
public class IntegrationStageStepParametersPMS implements StepParameters {
  String identifier;
  String name;
  ParameterField<String> description;
  List<NGVariable> variables;
  String type;
  Infrastructure infrastructure;
  List<DependencyElement> dependencies;
  ParameterField<List<String>> sharedPaths;
  ParameterField<String> skipCondition;
  ParameterField<Boolean> enableCloneRepo;
  BuildStatusUpdateParameter buildStatusUpdateParameter;
  String childNodeID;

  public static IntegrationStageStepParametersPMS getStepParameters(StageElementConfig stageElementConfig,
      String childNodeID, BuildStatusUpdateParameter buildStatusUpdateParameter, PlanCreationContext ctx) {
    if (stageElementConfig == null) {
      return IntegrationStageStepParametersPMS.builder().childNodeID(childNodeID).build();
    }
    IntegrationStageConfig integrationStageConfig = (IntegrationStageConfig) stageElementConfig.getStageType();

    Infrastructure infrastructure = integrationStageConfig.getInfrastructure();
    if (integrationStageConfig.getInfrastructure().getType() == Type.USE_FROM_STAGE) {
      UseFromStageInfraYaml useFromStageInfraYaml = (UseFromStageInfraYaml) integrationStageConfig.getInfrastructure();
      if (useFromStageInfraYaml.getUseFromStage() != null) {
        YamlField yamlField = ctx.getCurrentField();
        String identifier = useFromStageInfraYaml.getUseFromStage();
        IntegrationStageConfig integrationStage = getIntegrationStageConfig(yamlField, identifier);
        infrastructure = integrationStage.getInfrastructure();
      }
    }

    return IntegrationStageStepParametersPMS.builder()
        .identifier(stageElementConfig.getIdentifier())
        .name(stageElementConfig.getName())
        .buildStatusUpdateParameter(buildStatusUpdateParameter)
        .description(stageElementConfig.getDescription())
        .infrastructure(infrastructure)
        .dependencies(integrationStageConfig.getServiceDependencies())
        .type(stageElementConfig.getType())
        .skipCondition(integrationStageConfig.getSkipCondition())
        .variables(stageElementConfig.getVariables())
        .childNodeID(childNodeID)
        .sharedPaths(integrationStageConfig.getSharedPaths())
        .enableCloneRepo(integrationStageConfig.getCloneCodebase())
        .build();
  }

  private static IntegrationStageConfig getIntegrationStageConfig(YamlField yamlField, String identifier) {
    try {
      YamlField stageYamlField = PlanCreatorUtils.getStageConfig(yamlField, identifier);
      StageElementConfig stageElementConfig =
          YamlUtils.read(YamlUtils.writeYamlString(stageYamlField), StageElementConfig.class);
      return (IntegrationStageConfig) stageElementConfig.getStageType();

    } catch (Exception ex) {
      throw new CIStageExecutionException(
          "Failed to deserialize IntegrationStage for use from stage identifier: " + identifier, ex);
    }
  }
}
