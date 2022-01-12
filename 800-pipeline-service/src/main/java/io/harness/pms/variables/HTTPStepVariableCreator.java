/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.variables;

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
import io.harness.steps.StepSpecTypeConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class HTTPStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Collections.singleton(StepSpecTypeConstants.HTTP);
  }

  @Override
  protected void addVariablesInComplexObject(Map<String, YamlProperties> yamlPropertiesMap,
      Map<String, YamlOutputProperties> yamlOutputPropertiesMap, YamlNode yamlNode) {
    List<String> complexFields = new ArrayList<>();
    complexFields.add(YAMLFieldNameConstants.OUTPUT_VARIABLES);
    complexFields.add(YAMLFieldNameConstants.HEADERS);

    List<YamlField> fields = yamlNode.fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID) && !complexFields.contains(field.getName())) {
        addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
      }
    });

    YamlField outputVariablesField = yamlNode.getField(YAMLFieldNameConstants.OUTPUT_VARIABLES);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(outputVariablesField)) {
      addVariablesForOutputVariables(outputVariablesField, yamlOutputPropertiesMap);
    }
    YamlField headersField = yamlNode.getField(YAMLFieldNameConstants.HEADERS);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(headersField)) {
      addHeaderVariables(headersField, yamlPropertiesMap);
    }
  }

  private void addHeaderVariables(YamlField headersField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlNode> headerNodes = headersField.getNode().asArray();
    headerNodes.forEach(headerNode -> {
      YamlField keyField = headerNode.getField(YAMLFieldNameConstants.KEY);
      if (keyField != null) {
        addFieldToPropertiesMapUnderStep(keyField, yamlPropertiesMap);
      } else {
        throw new InvalidRequestException("Key in header field cannot be null or empty");
      }
    });
  }
}
