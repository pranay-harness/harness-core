/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.sdk.core.pipeline.variables;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
public class VariableCreatorHelper {
  public void addVariablesForVariables(
      YamlField variablesField, Map<String, YamlProperties> yamlPropertiesMap, String fieldName) {
    List<YamlNode> variableNodes = variablesField.getNode().asArray();
    variableNodes.forEach(variableNode -> {
      YamlField uuidNode = variableNode.getField(YAMLFieldNameConstants.UUID);
      if (uuidNode != null) {
        String fqn = YamlUtils.getFullyQualifiedName(uuidNode.getNode());
        String localName = YamlUtils.getQualifiedNameTillGivenField(uuidNode.getNode(), fieldName);
        YamlField valueNode = variableNode.getField(YAMLFieldNameConstants.VALUE);
        String variableName =
            Objects.requireNonNull(variableNode.getField(YAMLFieldNameConstants.NAME)).getNode().asText();
        if (valueNode == null) {
          throw new InvalidRequestException(
              "Variable with name \"" + variableName + "\" added without any value. Fqn: " + fqn);
        }
        yamlPropertiesMap.put(valueNode.getNode().getCurrJsonNode().textValue(),
            YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
      }
    });
  }

  public VariableCreationResponse createVariableResponseForVariables(YamlField variablesField, String fieldName) {
    Map<String, YamlProperties> yamlPropertiesMap = new LinkedHashMap<>();
    addVariablesForVariables(variablesField, yamlPropertiesMap, fieldName);
    return VariableCreationResponse.builder().yamlProperties(yamlPropertiesMap).build();
  }

  public void addFieldToPropertiesMap(
      YamlField fieldNode, Map<String, YamlProperties> yamlPropertiesMap, String fieldName) {
    String fqn = YamlUtils.getFullyQualifiedName(fieldNode.getNode());
    String localName = YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), fieldName);
    yamlPropertiesMap.put(fieldNode.getNode().getCurrJsonNode().textValue(),
        YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
  }

  public boolean isNotYamlFieldEmpty(YamlField yamlField) {
    if (yamlField == null) {
      return false;
    }
    return !(
        yamlField.getNode().fields().size() == 1 && yamlField.getNode().getField(YAMLFieldNameConstants.UUID) != null);
  }

  public List<YamlField> getStepYamlFields(List<YamlNode> yamlNodes) {
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

  public List<YamlField> getStepYamlFields(YamlField config) {
    List<YamlNode> yamlNodes =
        Optional
            .of(Preconditions.checkNotNull(config.getNode().getField(YAMLFieldNameConstants.STEPS)).getNode().asArray())
            .orElse(Collections.emptyList());
    return getStepYamlFields(yamlNodes);
  }
}
