package io.harness.beans.stages;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  Map<String, Object> variables;
  String type;
  ParameterField<Infrastructure> infrastructure;
  ParameterField<List<DependencyElement>> dependencies;
  ParameterField<List<String>> sharedPaths;
  ParameterField<String> skipCondition;
  ParameterField<String> workingDirectory;
  ParameterField<Boolean> skipGitClone;
  String childNodeID;

  public static IntegrationStageStepParametersPMS getStepParameters(
      StageElementConfig stageElementConfig, String childNodeID) {
    if (stageElementConfig == null) {
      return IntegrationStageStepParametersPMS.builder().childNodeID(childNodeID).build();
    }
    IntegrationStageConfig integrationStageConfig = (IntegrationStageConfig) stageElementConfig.getStageType();
    Map<String, Object> variablesMap = new HashMap<>();
    for (NGVariable variable : integrationStageConfig.getVariables()) {
      variablesMap.put(variable.getName(), variable.getValue());
    }
    return IntegrationStageStepParametersPMS.builder()
        .identifier(stageElementConfig.getIdentifier())
        .name(stageElementConfig.getName())
        .description(stageElementConfig.getDescription())
        .infrastructure(integrationStageConfig.getInfrastructure())
        .dependencies(integrationStageConfig.getDependencies())
        .workingDirectory(integrationStageConfig.getWorkingDirectory())
        .type(stageElementConfig.getType())
        .skipCondition(integrationStageConfig.getSkipCondition())
        .variables(variablesMap)
        .childNodeID(childNodeID)
        .sharedPaths(integrationStageConfig.getSharedPaths())
        .skipGitClone(integrationStageConfig.getSkipGitClone())
        .build();
  }
}
