package io.harness.cdng.creator.variables;

import io.harness.cdng.visitor.YamlTypes;
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
  protected void addVariablesForStepSpec(YamlField specField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<String> complexFields = new ArrayList<>();
    complexFields.add(YamlTypes.OUTPUT_VARIABLES);
    complexFields.add(YamlTypes.HEADERS);

    List<YamlField> fields = specField.getNode().fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID) && !complexFields.contains(field.getName())) {
        addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
      }
    });

    YamlField outputVariablesField = specField.getNode().getField(YamlTypes.OUTPUT_VARIABLES);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(outputVariablesField)) {
      VariableCreatorHelper.addVariablesForVariables(
          outputVariablesField, yamlPropertiesMap, YAMLFieldNameConstants.STEP);
    }
    YamlField headersField = specField.getNode().getField(YamlTypes.HEADERS);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(headersField)) {
      addHeaderVariables(headersField, yamlPropertiesMap);
    }
  }

  private void addHeaderVariables(YamlField headersField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlNode> headerNodes = headersField.getNode().asArray();
    headerNodes.forEach(headerNode -> {
      YamlField keyField = headerNode.getField(YAMLFieldNameConstants.KEY);
      VariableCreatorHelper.addFieldToPropertiesMap(keyField, yamlPropertiesMap, YAMLFieldNameConstants.STEP);
      YamlField valueField = headerNode.getField(YAMLFieldNameConstants.VALUE);
      VariableCreatorHelper.addFieldToPropertiesMap(valueField, yamlPropertiesMap, YAMLFieldNameConstants.STEP);
    });
  }
}
