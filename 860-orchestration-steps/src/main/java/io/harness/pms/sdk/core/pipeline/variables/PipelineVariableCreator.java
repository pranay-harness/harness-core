package io.harness.pms.sdk.core.pipeline.variables;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.ChildrenVariableCreator;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineVariableCreator extends ChildrenVariableCreator {
  @Override
  public LinkedHashMap<String, VariableCreationResponse> createVariablesForChildrenNodes(
      VariableCreationContext ctx, YamlField config) {
    LinkedHashMap<String, VariableCreationResponse> responseMap = new LinkedHashMap<>();
    YamlField stagesYamlNode = config.getNode().getField(YAMLFieldNameConstants.STAGES);
    if (stagesYamlNode != null) {
      getStageYamlFields(stagesYamlNode, responseMap);
    }
    return responseMap;
  }

  @Override
  public VariableCreationResponse createVariablesForParentNode(VariableCreationContext ctx, YamlField config) {
    YamlNode node = config.getNode();
    String pipelineUUID = node.getUuid();
    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();
    yamlPropertiesMap.put(pipelineUUID,
        YamlProperties.newBuilder()
            .setLocalName(YAMLFieldNameConstants.PIPELINE)
            .setFqn(YamlUtils.getFullyQualifiedName(node))
            .build());
    addVariablesForPipeline(yamlPropertiesMap, node);
    return VariableCreationResponse.builder().yamlProperties(yamlPropertiesMap).build();
  }

  private void addVariablesForPipeline(Map<String, YamlProperties> yamlPropertiesMap, YamlNode yamlNode) {
    YamlField nameField = yamlNode.getField(YAMLFieldNameConstants.NAME);
    if (nameField != null) {
      String nameFQN = YamlUtils.getFullyQualifiedName(nameField.getNode());
      yamlPropertiesMap.put(nameField.getNode().getCurrJsonNode().textValue(),
          YamlProperties.newBuilder().setLocalName(nameFQN).setFqn(nameFQN).build());
    }
    YamlField descriptionField = yamlNode.getField(YAMLFieldNameConstants.DESCRIPTION);
    if (descriptionField != null) {
      String descriptionFQN = YamlUtils.getFullyQualifiedName(descriptionField.getNode());
      yamlPropertiesMap.put(descriptionField.getNode().getCurrJsonNode().textValue(),
          YamlProperties.newBuilder().setLocalName(descriptionFQN).setFqn(descriptionFQN).build());
    }
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.PIPELINE, Collections.singleton("__any__"));
  }

  private void getStageYamlFields(
      YamlField stagesYamlNode, LinkedHashMap<String, VariableCreationResponse> responseMap) {
    List<YamlNode> yamlNodes = Optional.of(stagesYamlNode.getNode().asArray()).orElse(Collections.emptyList());
    List<YamlField> stageFields = new LinkedList<>();

    yamlNodes.forEach(yamlNode -> {
      YamlField stageField = yamlNode.getField(YAMLFieldNameConstants.STAGE);
      YamlField parallelStageField = yamlNode.getField(YAMLFieldNameConstants.PARALLEL);
      if (stageField != null) {
        stageFields.add(stageField);
      } else if (parallelStageField != null) {
        List<YamlField> childYamlFields = Optional.of(parallelStageField.getNode().asArray())
                                              .orElse(Collections.emptyList())
                                              .stream()
                                              .map(el -> el.getField(YAMLFieldNameConstants.STAGE))
                                              .filter(Objects::nonNull)
                                              .collect(Collectors.toList());
        if (EmptyPredicate.isNotEmpty(childYamlFields)) {
          stageFields.addAll(childYamlFields);
        }
      }
    });

    for (YamlField stageYamlField : stageFields) {
      Map<String, YamlField> stageYamlFieldMap = new HashMap<>();
      stageYamlFieldMap.put(stageYamlField.getNode().getUuid(), stageYamlField);
      responseMap.put(stageYamlField.getNode().getUuid(),
          VariableCreationResponse.builder().dependencies(stageYamlFieldMap).build());
    }
  }
}
