package io.harness.steps.jira;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.VariableCreatorHelper;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.StepSpecTypeConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class JiraStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    Set<String> strings = new HashSet<>();
    strings.add(StepSpecTypeConstants.JIRA_CREATE);
    return strings;
  }

  @Override
  protected void addVariablesInComplexObject(Map<String, YamlProperties> yamlPropertiesMap,
      Map<String, YamlOutputProperties> yamlOutputPropertiesMap, YamlNode yamlNode) {
    List<String> complexFields = new ArrayList<>();
    complexFields.add("fields");

    List<YamlField> fields = yamlNode.fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID) && !complexFields.contains(field.getName())) {
        addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
      }
    });

    YamlField yamlField = yamlNode.getField("fields");
    if (VariableCreatorHelper.isNotYamlFieldEmpty(yamlField)) {
      addVariablesForFields(yamlField, yamlPropertiesMap);
    }
  }

  private void addVariablesForFields(YamlField yamlField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlNode> yamlNodes = yamlField.getNode().asArray();
    yamlNodes.forEach(yamlNode -> {
      YamlField uuidNode = yamlNode.getField(YAMLFieldNameConstants.UUID);
      if (uuidNode != null) {
        addFieldToPropertiesMapUnderStep(uuidNode, yamlPropertiesMap);
      }
    });
  }
}
