package io.harness.beans.stages;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.toMap;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.CustomTextVariable;
import io.harness.beans.yaml.extended.CustomVariable;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

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
  Infrastructure infrastructure;
  List<DependencyElement> dependencies;
  ParameterField<List<String>> sharedPaths;
  ParameterField<String> skipCondition;
  ParameterField<Boolean> enableCloneRepo;
  String childNodeID;

  public static IntegrationStageStepParametersPMS getStepParameters(
      StageElementConfig stageElementConfig, String childNodeID) {
    if (stageElementConfig == null) {
      return IntegrationStageStepParametersPMS.builder().childNodeID(childNodeID).build();
    }
    IntegrationStageConfig integrationStageConfig = (IntegrationStageConfig) stageElementConfig.getStageType();
    Map<String, Object> variablesMap = new HashMap<>();
    if (isNotEmpty(integrationStageConfig.getVariables())) {
      integrationStageConfig.getVariables()
          .stream()
          .filter(customVariables -> customVariables.getType().equals(CustomVariable.Type.TEXT))
          .map(customVariable -> (CustomTextVariable) customVariable)
          .collect(toMap(CustomTextVariable::getName, CustomTextVariable::getValue));
    }

    return IntegrationStageStepParametersPMS.builder()
        .identifier(stageElementConfig.getIdentifier())
        .name(stageElementConfig.getName())
        .description(stageElementConfig.getDescription())
        .infrastructure(integrationStageConfig.getInfrastructure())
        .dependencies(integrationStageConfig.getServiceDependencies())
        .type(stageElementConfig.getType())
        .skipCondition(integrationStageConfig.getSkipCondition())
        .variables(variablesMap)
        .childNodeID(childNodeID)
        .sharedPaths(integrationStageConfig.getSharedPaths())
        .enableCloneRepo(integrationStageConfig.getCloneCodebase())
        .build();
  }
}
