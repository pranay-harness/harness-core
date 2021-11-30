package io.harness.steps.approval;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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

    YamlField approversField = yamlNode.getField(YamlTypes.APPROVERS);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(approversField)) {
      addVariablesForApprovers(approversField, yamlPropertiesMap);
    }

    YamlField inputsField = yamlNode.getField(YamlTypes.APPROVAL_INPUTS);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(inputsField)) {
      addVariablesForInputs(inputsField, yamlPropertiesMap);
    }
  }

  private void addVariablesForInputs(YamlField inputsField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlNode> nodes = inputsField.getNode().asArray();
    nodes.forEach(node -> {
      YamlField field = node.getField(YAMLFieldNameConstants.UUID);
      if (field != null) {
        String fqn = YamlUtils.getFullyQualifiedName(field.getNode());
        String localName;
        if (fqn.contains(YAMLFieldNameConstants.PIPELINE_INFRASTRUCTURE)) {
          localName = YamlUtils.getQualifiedNameTillGivenField(node, YAMLFieldNameConstants.PIPELINE_INFRASTRUCTURE);
        } else {
          localName = YamlUtils.getQualifiedNameTillGivenField(node, YAMLFieldNameConstants.EXECUTION);
        }
        yamlPropertiesMap.put(node.getField("defaultValue").getNode().getCurrJsonNode().textValue(),
            YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
      }
    });
  }

  private void addVariablesForApprovers(YamlField yamlField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlField> fields = yamlField.getNode().fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID)) {
        addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
      }
    });
  }
}
