/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.approval;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.VariableCreatorHelper;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.YamlTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class ApprovalStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    Set<String> strings = new HashSet<>();
    strings.add(StepSpecTypeConstants.HARNESS_APPROVAL);
    return strings;
  }

  @Override
  protected void addVariablesInComplexObject(Map<String, YamlProperties> yamlPropertiesMap,
      Map<String, YamlOutputProperties> yamlOutputPropertiesMap, YamlNode yamlNode) {
    List<String> complexFields = new ArrayList<>();
    complexFields.add(YamlTypes.APPROVAL_INPUTS);
    complexFields.add(YamlTypes.APPROVERS);

    List<YamlField> fields = yamlNode.fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID) && !complexFields.contains(field.getName())) {
        addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
      }
    });

    YamlField inputsField = yamlNode.getField(YamlTypes.APPROVAL_INPUTS);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(inputsField)) {
      addVariablesForInputs(inputsField, yamlPropertiesMap);
    }
  }

  private void addVariablesForInputs(YamlField inputsField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlNode> variableNodes = inputsField.getNode().asArray();
    variableNodes.forEach(variableNode -> {
      YamlField uuidNode = variableNode.getField(YAMLFieldNameConstants.UUID);
      if (uuidNode != null) {
        String original = YAMLFieldNameConstants.SPEC + "." + YamlTypes.APPROVAL_INPUTS;
        String replacement = YAMLFieldNameConstants.OUTPUT + "." + YamlTypes.APPROVAL_INPUTS;

        String fqn = YamlUtils.getFullyQualifiedName(uuidNode.getNode()).replace(original, replacement);
        String localName =
            YamlUtils.getQualifiedNameTillGivenField(uuidNode.getNode(), YAMLFieldNameConstants.EXECUTION)
                .replace(original, replacement);

        YamlField valueNode = variableNode.getField("defaultValue");
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
}
