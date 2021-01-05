package io.harness.pms.variables;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.variables.VariableMergeServiceResponse.VariableResponseMapValue;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VariableCreationBlobResponseUtils {
  public VariableMergeServiceResponse getMergeServiceResponse(String yaml, VariablesCreationBlobResponse response) {
    Map<String, VariableResponseMapValue> metadataMap = new HashMap<>();
    response.getYamlPropertiesMap().forEach(
        (k, v) -> metadataMap.put(k, VariableResponseMapValue.builder().yamlProperties(v).build()));
    return VariableMergeServiceResponse.builder().yaml(yaml).metadataMap(metadataMap).build();
  }

  public void mergeResponses(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (otherResponse == null) {
      return;
    }

    mergeYamlProperties(builder, otherResponse);
    mergeResolvedDependencies(builder, otherResponse);
    mergeDependencies(builder, otherResponse);
  }

  public void mergeYamlProperties(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (isNotEmpty(otherResponse.getYamlPropertiesMap())) {
      otherResponse.getYamlPropertiesMap().forEach(builder::putYamlProperties);
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
