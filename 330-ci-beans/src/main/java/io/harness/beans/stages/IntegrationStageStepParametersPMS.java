package io.harness.beans.stages;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure.Type;
import io.harness.beans.yaml.extended.infrastrucutre.UseFromStageInfraYaml;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("integrationStageStepParameters")
public class IntegrationStageStepParametersPMS implements SpecParameters {
  Infrastructure infrastructure;
  List<DependencyElement> dependencies;
  ParameterField<List<String>> sharedPaths;
  ParameterField<Boolean> enableCloneRepo;
  BuildStatusUpdateParameter buildStatusUpdateParameter;
  String childNodeID;

  public static IntegrationStageStepParametersPMS getStepParameters(StageElementConfig stageElementConfig,
      String childNodeID, BuildStatusUpdateParameter buildStatusUpdateParameter, PlanCreationContext ctx) {
    if (stageElementConfig == null) {
      return IntegrationStageStepParametersPMS.builder().childNodeID(childNodeID).build();
    }
    IntegrationStageConfig integrationStageConfig = (IntegrationStageConfig) stageElementConfig.getStageType();

    Infrastructure infrastructure = getInfrastructure(stageElementConfig, ctx);

    return IntegrationStageStepParametersPMS.builder()
        .buildStatusUpdateParameter(buildStatusUpdateParameter)
        .infrastructure(infrastructure)
        .dependencies(integrationStageConfig.getServiceDependencies())
        .childNodeID(childNodeID)
        .sharedPaths(integrationStageConfig.getSharedPaths())
        .enableCloneRepo(integrationStageConfig.getCloneCodebase())
        .build();
  }

  public static Infrastructure getInfrastructure(StageElementConfig stageElementConfig, PlanCreationContext ctx) {
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

    return infrastructure;
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
