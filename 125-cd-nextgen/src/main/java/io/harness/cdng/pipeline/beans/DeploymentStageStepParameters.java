package io.harness.cdng.pipeline.beans;

import io.harness.beans.ParameterField;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.variables.NGVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  Map<String, Object> variables;
  ParameterField<String> skipCondition;

  String childNodeID;

  public static DeploymentStageStepParameters getStepParameters(StageElementConfig config, String childNodeID) {
    if (config == null) {
      return DeploymentStageStepParameters.builder().childNodeID(childNodeID).build();
    }
    DeploymentStageConfig stageType = (DeploymentStageConfig) config.getStageType();
    Map<String, Object> variablesMap = new HashMap<>();
    for (NGVariable variable : stageType.getVariables()) {
      variablesMap.put(variable.getName(), variable.getValue());
    }
    return DeploymentStageStepParameters.builder()
        .identifier(config.getIdentifier())
        .name(config.getName())
        .description(config.getDescription())
        .failureStrategies(config.getFailureStrategies())
        .type(config.getType())
        .skipCondition(stageType.getSkipCondition())
        .variables(variablesMap)
        .childNodeID(childNodeID)
        .build();
  }
}
