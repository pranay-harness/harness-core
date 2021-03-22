package io.harness.cdng.creator.variables;

import io.harness.steps.shellScript.yaml.InfrastructureKind;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.variables.VariableCreatorHelper;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InfraVariableCreator {
  public VariableCreationResponse createVariableResponse(YamlField infraField) {
    if (infraField == null) {
      return VariableCreationResponse.builder().build();
    }

    Map<String, YamlProperties> yamlPropertiesMap = new LinkedHashMap<>();
    String infraUUID = infraField.getNode().getUuid();
    yamlPropertiesMap.put(infraUUID, YamlProperties.newBuilder().setFqn(YamlTypes.PIPELINE_INFRASTRUCTURE).build());

    YamlField infraDefNode = infraField.getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(infraDefNode)
        && VariableCreatorHelper.isNotYamlFieldEmpty(infraDefNode.getNode().getField(YamlTypes.SPEC))) {
      addVariablesForInfraDef(infraDefNode, yamlPropertiesMap);
    }
    YamlField envField = infraField.getNode().getField(YamlTypes.ENVIRONMENT_YAML);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(envField)) {
      addVariablesForEnv(envField, yamlPropertiesMap);
    }
    YamlField envRefField = infraField.getNode().getField(YamlTypes.ENVIRONMENT_REF);
    if (envRefField != null) {
      VariableCreatorHelper.addFieldToPropertiesMap(envRefField, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
    }
    return VariableCreationResponse.builder().yamlProperties(yamlPropertiesMap).build();
  }

  private void addVariablesForEnv(YamlField envNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField nameField = envNode.getNode().getField(YAMLFieldNameConstants.NAME);
    if (nameField != null) {
      VariableCreatorHelper.addFieldToPropertiesMap(nameField, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
    }
    YamlField descriptionNode = envNode.getNode().getField(YAMLFieldNameConstants.DESCRIPTION);
    if (descriptionNode != null) {
      VariableCreatorHelper.addFieldToPropertiesMap(
          descriptionNode, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
    }
    YamlField tagsField = envNode.getNode().getField(YAMLFieldNameConstants.TAGS);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(tagsField)) {
      List<YamlField> fields = tagsField.getNode().fields();
      fields.forEach(field -> {
        if (!field.getName().equals(YamlTypes.UUID)) {
          VariableCreatorHelper.addFieldToPropertiesMap(field, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
        }
      });
    }
  }

  private void addVariablesForInfraDef(YamlField infraDefNode, Map<String, YamlProperties> yamlPropertiesMap) {
    String type = infraDefNode.getNode().getType();
    if (type != null) {
      switch (type) {
        case InfrastructureKind.KUBERNETES_DIRECT:
          YamlField specNode = infraDefNode.getNode().getField(YamlTypes.SPEC);
          if (specNode != null) {
            addVariablesForKubernetesInfra(specNode, yamlPropertiesMap);
          }
          break;
        default:
          throw new InvalidRequestException("Invalid infra definition type");
      }
    }
  }

  private void addVariablesForKubernetesInfra(YamlField infraSpecNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField connectorRefNode = infraSpecNode.getNode().getField(YamlTypes.CONNECTOR_REF);
    if (connectorRefNode != null) {
      VariableCreatorHelper.addFieldToPropertiesMap(
          connectorRefNode, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
    }
    YamlField namespaceNode = infraSpecNode.getNode().getField(YamlTypes.NAMESPACE);
    if (namespaceNode != null) {
      VariableCreatorHelper.addFieldToPropertiesMap(
          namespaceNode, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
    }
    YamlField releaseNameNode = infraSpecNode.getNode().getField(YamlTypes.RELEASE_NAME);
    if (namespaceNode != null) {
      VariableCreatorHelper.addFieldToPropertiesMap(
          releaseNameNode, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
    }
  }
}
