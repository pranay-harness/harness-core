package io.harness.cdng.creator.variables;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class K8sStepVariableCreator extends GenericStepVariableCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Collections.singleton(StepSpecTypeConstants.K8S_ROLLING_DEPLOY);
  }

  @Override
  public VariableCreationResponse createVariablesForParentNode(VariableCreationContext ctx, YamlField config) {
    YamlNode node = config.getNode();
    String stepUUID = node.getUuid();
    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();
    yamlPropertiesMap.put(stepUUID,
        YamlProperties.newBuilder()
            .setLocalName(YAMLFieldNameConstants.STEP)
            .setFqn(YamlUtils.getFullyQualifiedName(node))
            .build());
    addVariablesForStep(yamlPropertiesMap, node);
    return VariableCreationResponse.builder().yamlProperties(yamlPropertiesMap).build();
  }

  private void addVariablesForStep(Map<String, YamlProperties> yamlPropertiesMap, YamlNode yamlNode) {
    YamlField nameField = yamlNode.getField(YAMLFieldNameConstants.NAME);
    if (nameField != null) {
      addFieldToPropertiesMapUnderStep(nameField, yamlPropertiesMap);
    }
    YamlField descriptionField = yamlNode.getField(YAMLFieldNameConstants.DESCRIPTION);
    if (descriptionField != null) {
      addFieldToPropertiesMapUnderStep(descriptionField, yamlPropertiesMap);
    }
    YamlField skipDryRun = yamlNode.getField(YamlTypes.SKIP_DRY_RUN);
    if (skipDryRun != null) {
      addFieldToPropertiesMapUnderStep(skipDryRun, yamlPropertiesMap);
    }
  }

  private void addFieldToPropertiesMapUnderStep(YamlField fieldNode, Map<String, YamlProperties> yamlPropertiesMap) {
    String fqn = YamlUtils.getFullyQualifiedName(fieldNode.getNode());
    String localName = YAMLFieldNameConstants.STEP + "."
        + YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), YAMLFieldNameConstants.STEP);
    yamlPropertiesMap.put(fieldNode.getNode().getCurrJsonNode().textValue(),
        YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
  }
}
