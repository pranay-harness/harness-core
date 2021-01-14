package io.harness.pms.sdk.core.pipeline.variables;

import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.ChildrenVariableCreator;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.*;

public abstract class GenericStepVariableCreator extends ChildrenVariableCreator {
  public abstract Set<String> getSupportedStepTypes();

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    Set<String> stepTypes = getSupportedStepTypes();
    if (EmptyPredicate.isEmpty(stepTypes)) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap(STEP, stepTypes);
  }

  @Override
  public VariableCreationResponse createVariablesForParentNode(VariableCreationContext ctx, YamlField config) {
    YamlNode node = config.getNode();
    String stepUUID = node.getUuid();
    Map<String, YamlProperties> yamlPropertiesMap = new LinkedHashMap<>();
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

    YamlField specField = yamlNode.getField(YAMLFieldNameConstants.SPEC);
    if (specField != null) {
      addVariablesForStepSpec(specField, yamlPropertiesMap);
    }
  }

  protected void addVariablesForStepSpec(YamlField specField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlField> fields = specField.getNode().fields();
    fields.forEach(field -> {
      if (!field.getName().equals(YAMLFieldNameConstants.UUID)) {
        addFieldToPropertiesMapUnderStep(field, yamlPropertiesMap);
      }
    });
  }

  @Override
  public LinkedHashMap<String, VariableCreationResponse> createVariablesForChildrenNodes(
      VariableCreationContext ctx, YamlField config) {
    return new LinkedHashMap<>();
  }

  protected void addFieldToPropertiesMapUnderStep(YamlField fieldNode, Map<String, YamlProperties> yamlPropertiesMap) {
    String fqn = YamlUtils.getFullyQualifiedName(fieldNode.getNode());
    String localName = YAMLFieldNameConstants.STEP + "."
        + YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), YAMLFieldNameConstants.STEP);
    yamlPropertiesMap.put(fieldNode.getNode().getCurrJsonNode().textValue(),
        YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
  }
}
