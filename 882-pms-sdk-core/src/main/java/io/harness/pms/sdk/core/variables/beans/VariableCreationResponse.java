/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.variables.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.VariablesCreationBlobResponse;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.creators.CreatorResponse;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class VariableCreationResponse implements CreatorResponse {
  @Singular Map<String, YamlProperties> yamlProperties;
  @Singular Map<String, YamlOutputProperties> yamlOutputProperties;
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

  private void addYamlProperty(String uuid, YamlProperties yamlProperty) {
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

  public void addYamlOutputProperties(Map<String, YamlOutputProperties> yamlOutputPropertiesMap) {
    if (EmptyPredicate.isEmpty(yamlOutputPropertiesMap)) {
      return;
    }
    yamlOutputPropertiesMap.forEach(this::addYamlOutputProperty);
  }

  private void addYamlOutputProperty(String uuid, YamlOutputProperties yamlOutputPropertyEntry) {
    if (yamlOutputProperties != null && yamlOutputProperties.containsKey(uuid)) {
      return;
    }
    if (this.yamlOutputProperties == null) {
      this.yamlOutputProperties = new HashMap<>();
    } else if (!(this.yamlOutputProperties instanceof HashMap)) {
      this.yamlOutputProperties = new HashMap<>(this.yamlOutputProperties);
    }
    this.yamlOutputProperties.put(uuid, yamlOutputPropertyEntry);
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
    if (isNotEmpty(yamlOutputProperties)) {
      for (Map.Entry<String, YamlOutputProperties> outputPropertiesEntry : yamlOutputProperties.entrySet()) {
        finalBuilder.putYamlOutputProperties(outputPropertiesEntry.getKey(), outputPropertiesEntry.getValue());
      }
    }
    return finalBuilder.build();
  }
}
