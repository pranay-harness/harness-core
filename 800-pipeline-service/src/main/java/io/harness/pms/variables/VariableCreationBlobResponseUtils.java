package io.harness.pms.variables;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
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
    response.getYamlPropertiesMap().forEach(
        (k, v) -> metadataMap.put(k, VariableResponseMapValue.builder().yamlProperties(v).build()));
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
        .errorResponses(hasSome(errorMessages) ? errorMessages : null)
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
    if (hasSome(otherResponse.getErrorResponseList())) {
      otherResponse.getErrorResponseList().forEach(builder::addErrorResponse);
    }
  }

  public void mergeYamlProperties(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (hasSome(otherResponse.getYamlPropertiesMap())) {
      otherResponse.getYamlPropertiesMap().forEach(builder::putYamlProperties);
    }
  }

  public void mergeResolvedDependencies(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (hasSome(otherResponse.getResolvedDependenciesMap())) {
      otherResponse.getResolvedDependenciesMap().forEach((key, value) -> {
        builder.putResolvedDependencies(key, value);
        builder.removeDependencies(key);
      });
    }
  }

  public void mergeDependencies(
      VariablesCreationBlobResponse.Builder builder, VariablesCreationBlobResponse otherResponse) {
    if (hasSome(otherResponse.getDependenciesMap())) {
      otherResponse.getDependenciesMap().forEach((key, value) -> {
        if (!builder.containsResolvedDependencies(key)) {
          builder.putDependencies(key, value);
        }
      });
    }
  }
}
