package io.harness.beans.stages;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.toMap;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.StringNGVariable;

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
  BuildStatusUpdateParameter buildStatusUpdateParameter;
  String childNodeID;

  public static IntegrationStageStepParametersPMS getStepParameters(StageElementConfig stageElementConfig,
      String childNodeID, BuildStatusUpdateParameter buildStatusUpdateParameter) {
    if (stageElementConfig == null) {
      return IntegrationStageStepParametersPMS.builder().childNodeID(childNodeID).build();
    }
    IntegrationStageConfig integrationStageConfig = (IntegrationStageConfig) stageElementConfig.getStageType();
    Map<String, Object> variablesMap = new HashMap<>();
    if (isNotEmpty(integrationStageConfig.getVariables())) {
      variablesMap = integrationStageConfig.getVariables()
                         .stream()
                         .filter(customVariables -> customVariables.getType() == NGVariableType.STRING)
                         .map(customVariable -> (StringNGVariable) customVariable)
                         .collect(toMap(StringNGVariable::getName, StringNGVariable::getValue));
    }

    return IntegrationStageStepParametersPMS.builder()
        .identifier(stageElementConfig.getIdentifier())
        .name(stageElementConfig.getName())
        .buildStatusUpdateParameter(buildStatusUpdateParameter)
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
