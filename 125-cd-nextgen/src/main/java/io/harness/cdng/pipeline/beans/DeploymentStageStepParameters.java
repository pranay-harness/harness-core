package io.harness.cdng.pipeline.beans;

import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("deploymentStageStepParameters")
public class DeploymentStageStepParameters implements StepParameters {
  String identifier;
  String name;
  ParameterField<String> description;
  List<FailureStrategyConfig> failureStrategies;
  String type;
  List<NGVariable> originalVariables;
  ParameterField<String> skipCondition;

  String childNodeID;

  public static DeploymentStageStepParameters getStepParameters(StageElementConfig config, String childNodeID) {
    if (config == null) {
      return DeploymentStageStepParameters.builder().childNodeID(childNodeID).build();
    }
    DeploymentStageConfig stageType = (DeploymentStageConfig) config.getStageType();

    return DeploymentStageStepParameters.builder()
        .identifier(config.getIdentifier())
        .name(config.getName())
        .description(config.getDescription())
        .failureStrategies(config.getFailureStrategies())
        .type(config.getType())
        .skipCondition(stageType.getSkipCondition())
        .originalVariables(stageType.getVariables())
        .childNodeID(childNodeID)
        .build();
  }
}
