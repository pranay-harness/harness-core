package io.harness.cdng.creator.variables;

import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InfraVariableCreator {
  public VariableCreationResponse createVariableResponse(YamlField infraField) {
    if (infraField == null) {
      return VariableCreationResponse.builder().build();
    }

    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();
    String infraUUID = infraField.getNode().getUuid();
    yamlPropertiesMap.put(infraUUID, YamlProperties.newBuilder().setFqn(YamlTypes.PIPELINE_INFRASTRUCTURE).build());

    YamlField infraDefNode = infraField.getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
    if (infraDefNode != null && infraDefNode.getNode().getField(YamlTypes.SPEC) != null) {
      addVariablesForInfraDef(infraDefNode, yamlPropertiesMap);
    }
    YamlField envField = infraField.getNode().getField(YamlTypes.ENVIRONMENT_YAML);
    if (envField != null) {
      addVariablesForEnv(envField, yamlPropertiesMap);
    }
    return VariableCreationResponse.builder().yamlProperties(yamlPropertiesMap).build();
  }

  private void addVariablesForEnv(YamlField envNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField nameField = envNode.getNode().getField(YAMLFieldNameConstants.NAME);
    if (nameField != null) {
      addFieldToPropertiesMapUnderInfra(nameField, yamlPropertiesMap);
    }
    YamlField descriptionNode = envNode.getNode().getField(YAMLFieldNameConstants.DESCRIPTION);
    if (descriptionNode != null) {
      addFieldToPropertiesMapUnderInfra(descriptionNode, yamlPropertiesMap);
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
      addFieldToPropertiesMapUnderInfra(connectorRefNode, yamlPropertiesMap);
    }
    YamlField namespaceNode = infraSpecNode.getNode().getField(YamlTypes.NAMESPACE);
    if (namespaceNode != null) {
      addFieldToPropertiesMapUnderInfra(namespaceNode, yamlPropertiesMap);
    }
    YamlField releaseNameNode = infraSpecNode.getNode().getField(YamlTypes.RELEASE_NAME);
    if (namespaceNode != null) {
      addFieldToPropertiesMapUnderInfra(releaseNameNode, yamlPropertiesMap);
    }
  }

  private void addFieldToPropertiesMapUnderInfra(YamlField fieldNode, Map<String, YamlProperties> yamlPropertiesMap) {
    String fqn = YamlUtils.getFullyQualifiedName(fieldNode.getNode());
    String localName = YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), YamlTypes.PIPELINE_INFRASTRUCTURE);
    yamlPropertiesMap.put(fieldNode.getNode().getCurrJsonNode().textValue(),
        YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
  }
}
