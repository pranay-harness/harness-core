package io.harness.pms.sdk.core.variables.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class VariableCreationResponse {
  @Singular Map<String, YamlProperties> yamlProperties;
  @Singular Map<String, YamlField> resolvedDependencies;
  @Singular Map<String, YamlField> dependencies;

  public void addResolvedDependencies(Map<String, YamlField> resolvedDependencies) {
    if (EmptyPredicate.isEmpty(resolvedDependencies)) {
      return;
    }
    resolvedDependencies.values().forEach(this::addResolvedDependency);
  }

  public void addResolvedDependency(YamlField yamlField) {
    if (resolvedDependencies == null) {
      resolvedDependencies = new HashMap<>();
    } else if (!(resolvedDependencies instanceof HashMap)) {
      resolvedDependencies = new HashMap<>(resolvedDependencies);
    }

    resolvedDependencies.put(yamlField.getNode().getUuid(), yamlField);
    if (dependencies != null) {
      dependencies.remove(yamlField.getNode().getUuid());
    }
  }

  public void addDependencies(Map<String, YamlField> fields) {
    if (EmptyPredicate.isEmpty(fields)) {
      return;
    }
    fields.values().forEach(this::addDependency);
  }

  public void addDependency(YamlField field) {
    String nodeId = field.getNode().getUuid();
    if (dependencies != null && dependencies.containsKey(nodeId)) {
      return;
    }

    if (dependencies == null) {
      dependencies = new HashMap<>();
    } else if (!(dependencies instanceof HashMap)) {
      dependencies = new HashMap<>(dependencies);
    }
    dependencies.put(nodeId, field);
  }

  public void addYamlProperties(Map<String, YamlProperties> yamlProperties) {
    if (EmptyPredicate.isEmpty(yamlProperties)) {
      return;
    }
    yamlProperties.forEach(this::addYamlProperty);
  }

  public void addYamlProperty(String uuid, YamlProperties yamlProperty) {
    if (yamlProperties != null && yamlProperties.containsKey(uuid)) {
      return;
    }
    if (yamlProperties == null) {
      yamlProperties = new HashMap<>();
    } else if (!(yamlProperties instanceof HashMap)) {
      yamlProperties = new HashMap<>(yamlProperties);
    }
    yamlProperties.put(uuid, yamlProperty);
  }

  public VariablesCreationBlobResponse toBlobResponse() {
    VariablesCreationBlobResponse.Builder finalBuilder = VariablesCreationBlobResponse.newBuilder();

    if (isNotEmpty(dependencies)) {
      for (Map.Entry<String, YamlField> dependency : dependencies.entrySet()) {
        finalBuilder.putDependencies(dependency.getKey(), dependency.getValue().toFieldBlob());
      }
    }

    if (isNotEmpty(resolvedDependencies)) {
      for (Map.Entry<String, YamlField> dependency : resolvedDependencies.entrySet()) {
        finalBuilder.putResolvedDependencies(dependency.getKey(), dependency.getValue().toFieldBlob());
      }
    }

    if (isNotEmpty(yamlProperties)) {
      for (Map.Entry<String, YamlProperties> yamlPropertiesEntry : yamlProperties.entrySet()) {
        finalBuilder.putYamlProperties(yamlPropertiesEntry.getKey(), yamlPropertiesEntry.getValue());
      }
    }
    return finalBuilder.build();
  }
}
