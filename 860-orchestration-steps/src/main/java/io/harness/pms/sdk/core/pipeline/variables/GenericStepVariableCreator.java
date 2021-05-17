package io.harness.pms.sdk.core.pipeline.variables;

import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
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
import java.util.Objects;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
@TargetModule(HarnessModule._882_PMS_SDK_CORE)
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

    YamlField timeoutField = yamlNode.getField(YAMLFieldNameConstants.TIMEOUT);
    if (timeoutField != null) {
      addFieldToPropertiesMapUnderStep(timeoutField, yamlPropertiesMap);
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
    String localName;
    if (fqn.contains(YAMLFieldNameConstants.PIPELINE_INFRASTRUCTURE)) {
      localName =
          YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), YAMLFieldNameConstants.PIPELINE_INFRASTRUCTURE);
    } else {
      localName = YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), YAMLFieldNameConstants.EXECUTION);
    }

    yamlPropertiesMap.put(fieldNode.getNode().getCurrJsonNode().textValue(),
        YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
  }

  protected void addVariablesForVariables(YamlField variablesField, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlNode> variableNodes = variablesField.getNode().asArray();
    variableNodes.forEach(variableNode -> {
      YamlField uuidNode = variableNode.getField(YAMLFieldNameConstants.UUID);
      if (uuidNode != null) {
        String fqn = YamlUtils.getFullyQualifiedName(uuidNode.getNode());
        String localName =
            YamlUtils.getQualifiedNameTillGivenField(uuidNode.getNode(), YAMLFieldNameConstants.EXECUTION);
        YamlField valueNode = variableNode.getField(YAMLFieldNameConstants.VALUE);
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