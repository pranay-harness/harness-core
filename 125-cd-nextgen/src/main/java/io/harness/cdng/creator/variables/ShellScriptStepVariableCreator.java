package io.harness.cdng.creator.variables;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.VariableCreatorHelper;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.common.script.ShellScriptSourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShellScriptStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Collections.singleton(StepSpecTypeConstants.SHELL_SCRIPT);
  }

  @Override
  protected void addVariablesInComplexObject(Map<String, YamlProperties> yamlPropertiesMap,
      Map<String, YamlOutputProperties> yamlOutputPropertiesMap, YamlNode yamlNode) {
    List<String> complexFields = new ArrayList<>();
    complexFields.add(YamlTypes.SOURCE);
    complexFields.add(YamlTypes.EXECUTION_TARGET);
    complexFields.add(YamlTypes.ENVIRONMENT_VARIABLES);
    complexFields.add(YamlTypes.OUTPUT_VARIABLES);

    List<YamlField> fields = yamlNode.fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID) && !complexFields.contains(field.getName())) {
        addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
      }
    });

    YamlField sourceField = yamlNode.getField(YamlTypes.SOURCE);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(sourceField)) {
      addVariablesForSourceField(sourceField, yamlPropertiesMap);
    }

    YamlField executionTargetField = yamlNode.getField(YamlTypes.EXECUTION_TARGET);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(executionTargetField)) {
      addVariablesForExecutionTargetField(executionTargetField, yamlPropertiesMap);
    }

    YamlField environmentVariablesField = yamlNode.getField(YamlTypes.ENVIRONMENT_VARIABLES);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(environmentVariablesField)) {
      addVariablesForVariables(environmentVariablesField, yamlPropertiesMap);
    }

    YamlField outputVariablesField = yamlNode.getField(YamlTypes.OUTPUT_VARIABLES);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(outputVariablesField)) {
      addVariablesForVariables(outputVariablesField, yamlPropertiesMap);
    }
  }

  private void addVariablesForSourceField(YamlField sourceField, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField typeField = sourceField.getNode().getField(YamlNode.TYPE_FIELD_NAME);
    if (typeField != null) {
      YamlField specField = sourceField.getNode().getField(YamlTypes.SPEC);
      switch (typeField.getNode().getCurrJsonNode().textValue()) {
        case ShellScriptSourceType.GIT:
        case ShellScriptSourceType.INLINE:
          if (specField != null) {
            List<YamlField> fields = specField.getNode().fields();
            fields.forEach(field -> {
              if (!field.getName().equals(YAMLFieldNameConstants.UUID)) {
                addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
              }
            });
          }
          break;
        default:
          throw new InvalidRequestException("Invalid source type");
      }
    }
  }

  private void addVariablesForExecutionTargetField(
      YamlField executionTargetField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlField> fields = executionTargetField.getNode().fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID)) {
        addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
      }
    });
  }
}
