package io.harness.pms.sdk.core.pipeline.variables;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.variables.ChildrenVariableCreator;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import com.google.common.base.Preconditions;
import java.util.*;
import java.util.stream.Collectors;

public class ExecutionVariableCreator extends ChildrenVariableCreator {
  @Override
  public LinkedHashMap<String, VariableCreationResponse> createVariablesForChildrenNodes(
      VariableCreationContext ctx, YamlField config) {
    LinkedHashMap<String, VariableCreationResponse> responseMap = new LinkedHashMap<>();
    List<YamlField> stepYamlFields = getStepYamlFields(config);
    for (YamlField stepYamlField : stepYamlFields) {
      Map<String, YamlField> stepYamlFieldMap = new HashMap<>();
      stepYamlFieldMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
      responseMap.put(
          stepYamlField.getNode().getUuid(), VariableCreationResponse.builder().dependencies(stepYamlFieldMap).build());
    }

    YamlField rollbackStepsField = config.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
    if (rollbackStepsField != null) {
      Map<String, YamlField> rollbackDependencyMap = new HashMap<>();
      rollbackDependencyMap.put(rollbackStepsField.getNode().getUuid(), rollbackStepsField);
      responseMap.put(rollbackStepsField.getNode().getUuid(),
          VariableCreationResponse.builder().dependencies(rollbackDependencyMap).build());
    }
    return responseMap;
  }

  @Override
  public VariableCreationResponse createVariablesForParentNode(VariableCreationContext ctx, YamlField config) {
    return VariableCreationResponse.builder().build();
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.EXECUTION, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  private List<YamlField> getStepYamlFields(YamlField config) {
    List<YamlNode> yamlNodes =
        Optional
            .of(Preconditions.checkNotNull(config.getNode().getField(YAMLFieldNameConstants.STEPS)).getNode().asArray())
            .orElse(Collections.emptyList());
    List<YamlField> stepFields = new LinkedList<>();

    yamlNodes.forEach(yamlNode -> {
      YamlField stepField = yamlNode.getField(YAMLFieldNameConstants.STEP);
      YamlField stepGroupField = yamlNode.getField(YAMLFieldNameConstants.STEP_GROUP);
      YamlField parallelStepField = yamlNode.getField(YAMLFieldNameConstants.PARALLEL);
      if (stepField != null) {
        stepFields.add(stepField);
      } else if (stepGroupField != null) {
        stepFields.add(stepGroupField);
      } else if (parallelStepField != null) {
        List<YamlField> childYamlFields = Optional.of(parallelStepField.getNode().asArray())
                                              .orElse(Collections.emptyList())
                                              .stream()
                                              .map(el -> el.getField(YAMLFieldNameConstants.STEP))
                                              .filter(Objects::nonNull)
                                              .collect(Collectors.toList());
        childYamlFields.addAll(Optional.of(parallelStepField.getNode().asArray())
                                   .orElse(Collections.emptyList())
                                   .stream()
                                   .map(el -> el.getField(YAMLFieldNameConstants.STEP_GROUP))
                                   .filter(Objects::nonNull)
                                   .collect(Collectors.toList()));
        if (EmptyPredicate.isNotEmpty(childYamlFields)) {
          stepFields.addAll(childYamlFields);
        }
      }
    });
    return stepFields;
  }
}
