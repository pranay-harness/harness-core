package io.harness.cdng.creator.variables;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.service.beans.ServiceSpecType;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ServiceVariableCreator {
  public VariableCreationResponse createVariableResponse(YamlField serviceField) {
    if (serviceField == null) {
      return VariableCreationResponse.builder().build();
    }
    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();
    String serviceUUID = serviceField.getNode().getUuid();
    yamlPropertiesMap.put(serviceUUID, YamlProperties.newBuilder().setFqn(YamlTypes.SERVICE_CONFIG).build());
    YamlField nameField = serviceField.getNode().getField(YAMLFieldNameConstants.NAME);
    if (nameField != null) {
      addFieldToPropertiesMapUnderService(nameField, yamlPropertiesMap);
    }
    YamlField descriptionField = serviceField.getNode().getField(YAMLFieldNameConstants.DESCRIPTION);
    if (descriptionField != null) {
      addFieldToPropertiesMapUnderService(descriptionField, yamlPropertiesMap);
    }

    YamlField serviceDefNode = serviceField.getNode().getField(YamlTypes.SERVICE_DEFINITION);
    if (serviceDefNode != null && serviceDefNode.getNode().getField(YamlTypes.SERVICE_SPEC) != null) {
      addVariablesForServiceSpec(serviceDefNode, yamlPropertiesMap);
    }
    return VariableCreationResponse.builder().yamlProperties(yamlPropertiesMap).build();
  }

  private void addVariablesForServiceSpec(YamlField serviceDefNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField typeField = serviceDefNode.getNode().getField(YAMLFieldNameConstants.TYPE);
    if (typeField != null) {
      switch (typeField.getNode().getCurrJsonNode().textValue()) {
        case ServiceSpecType.KUBERNETES:
          YamlField specNode = serviceDefNode.getNode().getField(YamlTypes.SERVICE_SPEC);
          if (specNode != null) {
            addVariablesForKubernetesServiceSpec(specNode, yamlPropertiesMap);
          }
          break;
        default:
          throw new InvalidRequestException("Invalid service type");
      }
    }
  }

  private void addVariablesForKubernetesServiceSpec(
      YamlField serviceSpecNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField artifactsNode = serviceSpecNode.getNode().getField(YamlTypes.ARTIFACT_LIST_CONFIG);
    if (artifactsNode != null) {
      addVariablesForArtifacts(artifactsNode, yamlPropertiesMap);
    }
    YamlField manifestsNode = serviceSpecNode.getNode().getField(YamlTypes.MANIFEST_LIST_CONFIG);
    if (manifestsNode != null) {
      addVariablesForManifests(manifestsNode, yamlPropertiesMap);
    }
  }

  private void addVariablesForArtifacts(YamlField artifactsNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField primaryNode = artifactsNode.getNode().getField(YamlTypes.PRIMARY_ARTIFACT);
    if (primaryNode != null) {
      addVariablesForPrimaryArtifact(primaryNode, yamlPropertiesMap);
    }
  }

  private void addVariablesForManifests(YamlField manifestsNode, Map<String, YamlProperties> yamlPropertiesMap) {
    List<YamlNode> manifestNodes = Optional.of(manifestsNode.getNode().asArray()).orElse(Collections.emptyList());
    for (YamlNode manifestNode : manifestNodes) {
      YamlField field = manifestNode.getField(YamlTypes.MANIFEST_CONFIG);
      if (field != null) {
        addVariablesForManifest(field, yamlPropertiesMap);
      }
    }
  }

  private void addVariablesForManifest(YamlField manifestNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField specNode = manifestNode.getNode().getField(YamlTypes.SPEC);
    if (specNode == null) {
      throw new InvalidRequestException("Invalid manifest config");
    }
    switch (manifestNode.getNode().getType()) {
      case ManifestType.K8Manifest:
        addVariablesForK8sManifest(specNode, yamlPropertiesMap);
        break;
      case ManifestType.VALUES:
        addVariablesForValuesManifest(specNode, yamlPropertiesMap);
        break;
      default:
        throw new InvalidRequestException("Invalid manifest type");
    }
  }

  private void addVariablesForK8sManifest(YamlField manifestSpecNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField storeNode = manifestSpecNode.getNode().getField(YamlTypes.STORE_CONFIG_WRAPPER);
    if (storeNode != null) {
      YamlField specNode = storeNode.getNode().getField(YamlTypes.SPEC);
      if (specNode == null) {
        throw new InvalidRequestException("Invalid store config");
      }
      switch (storeNode.getNode().getType()) {
        case ManifestStoreType.GIT:
          addVariablesForGit(specNode, yamlPropertiesMap);
          break;
        default:
          throw new InvalidRequestException("Invalid store type");
      }
    }
  }

  private void addVariablesForValuesManifest(
      YamlField manifestSpecNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField storeNode = manifestSpecNode.getNode().getField(YamlTypes.STORE_CONFIG_WRAPPER);
    if (storeNode != null) {
      YamlField specNode = storeNode.getNode().getField(YamlTypes.SPEC);
      if (specNode == null) {
        throw new InvalidRequestException("Invalid store config");
      }
      switch (storeNode.getNode().getType()) {
        case ManifestStoreType.GIT:
          addVariablesForGit(specNode, yamlPropertiesMap);
          break;
        default:
          throw new InvalidRequestException("Invalid store type");
      }
    }
  }

  private void addVariablesForGit(YamlField gitNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField connectorRefNode = gitNode.getNode().getField(YamlTypes.CONNECTOR_REF);
    if (connectorRefNode != null) {
      addFieldToPropertiesMapUnderService(connectorRefNode, yamlPropertiesMap);
    }
    YamlField branchField = gitNode.getNode().getField(YamlTypes.BRANCH);
    if (branchField != null) {
      addFieldToPropertiesMapUnderService(branchField, yamlPropertiesMap);
    }
    YamlField commitIDField = gitNode.getNode().getField(YamlTypes.COMMIT_ID);
    if (commitIDField != null) {
      addFieldToPropertiesMapUnderService(commitIDField, yamlPropertiesMap);
    }
  }

  private void addVariablesForPrimaryArtifact(
      YamlField primaryArtifactNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField specNode = primaryArtifactNode.getNode().getField(YamlTypes.SPEC);
    if (specNode == null) {
      throw new InvalidRequestException("Invalid artifact config");
    }
    switch (primaryArtifactNode.getNode().getType()) {
      case "Dockerhub":
        addVariablesForDockerArtifact(specNode, yamlPropertiesMap);
        break;
      default:
        throw new InvalidRequestException("Invalid primary artifact type");
    }
  }

  private void addVariablesForDockerArtifact(
      YamlField artifactSpecNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField connectorRefNode = artifactSpecNode.getNode().getField(YamlTypes.CONNECTOR_REF);
    if (connectorRefNode != null) {
      addFieldToPropertiesMapUnderService(connectorRefNode, yamlPropertiesMap);
    }
    YamlField imagePathField = artifactSpecNode.getNode().getField(YamlTypes.IMAGE_PATH);
    if (imagePathField != null) {
      addFieldToPropertiesMapUnderService(imagePathField, yamlPropertiesMap);
    }
    YamlField tagField = artifactSpecNode.getNode().getField(YamlTypes.TAG);
    if (tagField != null) {
      addFieldToPropertiesMapUnderService(tagField, yamlPropertiesMap);
    }
  }

  private void addFieldToPropertiesMapUnderService(YamlField fieldNode, Map<String, YamlProperties> yamlPropertiesMap) {
    String fqn = YamlUtils.getFullyQualifiedName(fieldNode.getNode());
    String localName = YamlUtils.getQualifiedNameTillGivenField(fieldNode.getNode(), YamlTypes.SERVICE_CONFIG);
    yamlPropertiesMap.put(fieldNode.getNode().getCurrJsonNode().textValue(),
        YamlProperties.newBuilder().setLocalName(localName).setFqn(fqn).build());
  }
}
