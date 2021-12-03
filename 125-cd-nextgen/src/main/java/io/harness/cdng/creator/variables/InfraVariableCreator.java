package io.harness.cdng.creator.variables;

import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.variables.VariableCreatorHelper;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import java.util.HashMap;
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
    Map<String, YamlField> dependenciesMap = new LinkedHashMap<>();
    String infraUUID = infraField.getNode().getUuid();
    yamlPropertiesMap.put(infraUUID, YamlProperties.newBuilder().setFqn(YamlTypes.PIPELINE_INFRASTRUCTURE).build());

    YamlField infraDefNode = infraField.getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(infraDefNode)
        && VariableCreatorHelper.isNotYamlFieldEmpty(infraDefNode.getNode().getField(YamlTypes.SPEC))) {
      addVariablesForInfraDef(infraDefNode, yamlPropertiesMap);

      YamlField provisionerField = infraDefNode.getNode().getField(YAMLFieldNameConstants.PROVISIONER);
      if (provisionerField != null) {
        dependenciesMap.putAll(addDependencyForProvisionerSteps(provisionerField));
      }
    }
    YamlField envField = infraField.getNode().getField(YamlTypes.ENVIRONMENT_YAML);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(envField)) {
      addVariablesForEnv(envField, yamlPropertiesMap);
    }
    YamlField envRefField = infraField.getNode().getField(YamlTypes.ENVIRONMENT_REF);
    if (envRefField != null) {
      VariableCreatorHelper.addFieldToPropertiesMap(envRefField, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
    }
    return VariableCreationResponse.builder()
        .yamlProperties(yamlPropertiesMap)
        .dependencies(DependenciesUtils.toDependenciesProto(dependenciesMap))
        .build();
  }

  private static Map<String, YamlField> addDependencyForProvisionerSteps(YamlField provisionerField) {
    Map<String, YamlField> stepsDependencyMap = new HashMap<>();
    List<YamlField> stepYamlFields = VariableCreatorHelper.getStepYamlFields(provisionerField);
    for (YamlField stepYamlField : stepYamlFields) {
      stepsDependencyMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
    }

    YamlField rollbackStepsField = provisionerField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
    if (rollbackStepsField != null) {
      List<YamlNode> yamlNodes = rollbackStepsField.getNode().asArray();
      List<YamlField> rollbackStepYamlFields = VariableCreatorHelper.getStepYamlFields(yamlNodes);
      for (YamlField stepYamlField : rollbackStepYamlFields) {
        stepsDependencyMap.put(stepYamlField.getNode().getUuid(), stepYamlField);
      }
    }
    return stepsDependencyMap;
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
          addVariablesForKubernetesInfra(infraDefNode, yamlPropertiesMap);
          break;

        case InfrastructureKind.KUBERNETES_GCP:
          addVariablesForKubernetesGcpInfra(infraDefNode, yamlPropertiesMap);
          break;

        default:
          throw new InvalidRequestException("Invalid infra definition type");
      }
    }
  }

  private void addVariablesForKubernetesInfra(YamlField infraDefNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField infraSpecNode = infraDefNode.getNode().getField(YamlTypes.SPEC);
    if (infraSpecNode == null) {
      return;
    }

    addVariableForYamlType(YamlTypes.CONNECTOR_REF, infraSpecNode, yamlPropertiesMap);
    addVariableForYamlType(YamlTypes.NAMESPACE, infraSpecNode, yamlPropertiesMap);
    addVariableForYamlType(YamlTypes.RELEASE_NAME, infraSpecNode, yamlPropertiesMap);
  }

  private void addVariablesForKubernetesGcpInfra(
      YamlField infraDefNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField infraSpecNode = infraDefNode.getNode().getField(YamlTypes.SPEC);
    if (infraSpecNode == null) {
      return;
    }

    addVariableForYamlType(YamlTypes.CONNECTOR_REF, infraSpecNode, yamlPropertiesMap);
    addVariableForYamlType(YamlTypes.NAMESPACE, infraSpecNode, yamlPropertiesMap);
    addVariableForYamlType(YamlTypes.RELEASE_NAME, infraSpecNode, yamlPropertiesMap);
    addVariableForYamlType(YamlTypes.CLUSTER, infraSpecNode, yamlPropertiesMap);
  }

  private void addVariableForYamlType(
      String yamlTypes, YamlField infraSpecNode, Map<String, YamlProperties> yamlPropertiesMap) {
    YamlField yamlNode = infraSpecNode.getNode().getField(yamlTypes);
    if (yamlNode != null) {
      VariableCreatorHelper.addFieldToPropertiesMap(yamlNode, yamlPropertiesMap, YamlTypes.PIPELINE_INFRASTRUCTURE);
    }
  }
}
