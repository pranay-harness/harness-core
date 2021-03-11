package io.harness.cdng.creator.variables;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.VariableCreatorHelper;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HTTPStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Collections.singleton(StepSpecTypeConstants.HTTP);
  }

  @Override
  protected void addVariablesInComplexObject(Map<String, YamlProperties> yamlPropertiesMap, YamlNode yamlNode) {
    List<String> complexFields = new ArrayList<>();
    complexFields.add(YamlTypes.OUTPUT_VARIABLES);
    complexFields.add(YamlTypes.HEADERS);

    List<YamlField> fields = yamlNode.fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID) && !complexFields.contains(field.getName())) {
        addFieldToPropertiesMapUnderStep(field, yamlNode, yamlPropertiesMap);
      }
    });

    YamlField outputVariablesField = yamlNode.getField(YamlTypes.OUTPUT_VARIABLES);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(outputVariablesField)) {
      VariableCreatorHelper.addVariablesForVariables(
          outputVariablesField, yamlPropertiesMap, findFieldNameForLocalName(yamlNode));
    }
    YamlField headersField = yamlNode.getField(YamlTypes.HEADERS);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(headersField)) {
      addHeaderVariables(headersField, yamlPropertiesMap);
    }
  }

  private void addHeaderVariables(YamlField headersField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlNode> headerNodes = headersField.getNode().asArray();
    headerNodes.forEach(headerNode -> {
      YamlField keyField = headerNode.getField(YAMLFieldNameConstants.KEY);
      if (keyField != null) {
        addFieldToPropertiesMapUnderStep(keyField, headerNode, yamlPropertiesMap);
      } else {
        throw new InvalidRequestException("Key in header field cannot be null or empty");
      }
    });
  }
}
