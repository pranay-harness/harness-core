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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
      addFieldToPropertiesMapUnderStep(nameField, yamlNode, yamlPropertiesMap);
    }
    YamlField descriptionField = yamlNode.getField(YAMLFieldNameConstants.DESCRIPTION);
    if (descriptionField != null) {
      addFieldToPropertiesMapUnderStep(descriptionField, yamlNode, yamlPropertiesMap);
    }

    YamlField timeoutField = yamlNode.getField(YAMLFieldNameConstants.TIMEOUT);
    if (timeoutField != null) {
      addFieldToPropertiesMapUnderStep(timeoutField, yamlNode, yamlPropertiesMap);
    }

    YamlField specField = yamlNode.getField(YAMLFieldNameConstants.SPEC);
    if (specField != null) {
      addVariablesInComplexObject(yamlPropertiesMap, specField.getNode());
    }
  }

  protected void addVariablesInComplexObject(Map<String, YamlProperties> yamlPropertiesMap, YamlNode yamlNode) {
    List<String> extraFields = new ArrayList<>();
    extraFields.add(YAMLFieldNameConstants.UUID);
    extraFields.add(YAMLFieldNameConstants.IDENTIFIER);
    extraFields.add(YAMLFieldNameConstants.TYPE);
    List<YamlField> fields = yamlNode.fields();
    fields.forEach(field -> {
      if (field.getNode().isObject()) {
        addVariablesInComplexObject(yamlPropertiesMap, field.getNode());
      } else if (field.getNode().isArray()) {
        List<YamlNode> innerFields = field.getNode().asArray();
        innerFields.forEach(f -> addVariablesInComplexObject(yamlPropertiesMap, f));
      } else if (!extraFields.contains(field.getName())) {
        addFieldToPropertiesMapUnderStep(field, yamlNode, yamlPropertiesMap);
      }
    });
  }

  @Override
  public LinkedHashMap<String, VariableCreationResponse> createVariablesForChildrenNodes(
      VariableCreationContext ctx, YamlField config) {
    return new LinkedHashMap<>();
  }

  protected void addFieldToPropertiesMapUnderStep(
      YamlField fieldNode, YamlNode fieldParentNode, Map<String, YamlProperties> yamlPropertiesMap) {
    String fqn = YamlUtils.getFullyQualifiedName(fieldNode.getNode());
    String localName;
    if (YamlUtils.findParentNode(fieldParentNode, YAMLFieldNameConstants.ROLLBACK_STEPS) != null) {
      localName = YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), YAMLFieldNameConstants.ROLLBACK_STEPS);
    } else {
      localName = YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), YAMLFieldNameConstants.STEPS);
    }

    yamlPropertiesMap.put(fieldNode.getNode().getCurrJsonNode().textValue(),
        YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
  }
}
