package io.harness.pms.variables;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.variables.VariableMergeServiceResponse.VariableResponseMapValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VariableCreationBlobResponseUtils {
  public VariableMergeServiceResponse getMergeServiceResponse(String yaml, VariablesCreationBlobResponse response) {
    Map<String, VariableResponseMapValue> metadataMap = new LinkedHashMap<>();
    // Add Yaml Properties
    response.getYamlPropertiesMap().forEach(
        (k, v) -> metadataMap.put(k, VariableResponseMapValue.builder().yamlProperties(v).build()));

    // Add Yaml Output Properties
    response.getYamlOutputPropertiesMap().keySet().forEach(uuid -> {
      YamlOutputProperties yamlOutputProperties = response.getYamlOutputPropertiesMap().get(uuid);
      if (metadataMap.containsKey(uuid)) {
        metadataMap.get(uuid).setYamlOutputProperties(yamlOutputProperties);
      } else {
        metadataMap.put(uuid, VariableResponseMapValue.builder().yamlOutputProperties(yamlOutputProperties).build());
      }
    });
    List<String> errorMessages = new ArrayList<>();
    response.getErrorResponseList().forEach(error -> {
      int messagesCount = error.getMessagesCount();
      for (int i = 0; i < messagesCount; i++) {
        errorMessages.add(error.getMessages(i));
      }
    });
    return VariableMergeServiceResponse.builder()
        .yaml(yaml)
        .metadataMap(metadataMap)
        .errorResponses(isNotEmpty(errorMessages) ? errorMessages : null)
        .build();
  }

  public void mergeResponses(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (otherResponse == null) {
      return;
    }

    mergeYamlProperties(builder, otherResponse);
    mergeResolvedDependencies(builder, otherResponse);
    mergeDependencies(builder, otherResponse);
    mergeErrorResponses(builder, otherResponse);
  }

  public static void mergeErrorResponses(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (isNotEmpty(otherResponse.getErrorResponseList())) {
      otherResponse.getErrorResponseList().forEach(builder::addErrorResponse);
    }
  }

  public void mergeYamlProperties(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (isNotEmpty(otherResponse.getYamlPropertiesMap())) {
      otherResponse.getYamlPropertiesMap().forEach(builder::putYamlProperties);
    }
  }

  public void mergeYamlOutputProperties(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (isNotEmpty(otherResponse.getYamlOutputPropertiesMap())) {
      otherResponse.getYamlOutputPropertiesMap().forEach(builder::putYamlOutputProperties);
    }
  }

  public void mergeResolvedDependencies(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (isNotEmpty(otherResponse.getResolvedDependenciesMap())) {
      otherResponse.getResolvedDependenciesMap().forEach((key, value) -> {
        builder.putResolvedDependencies(key, value);
        builder.removeDependencies(key);
      });
    }
  }

  public void mergeDependencies(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (isNotEmpty(otherResponse.getDependenciesMap())) {
      otherResponse.getDependenciesMap().forEach((key, value) -> {
        if (!builder.containsResolvedDependencies(key)) {
          builder.putDependencies(key, value);
        }
      });
    }
  }
}
