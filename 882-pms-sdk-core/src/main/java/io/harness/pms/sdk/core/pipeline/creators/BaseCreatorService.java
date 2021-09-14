/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.pipeline.creators;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.yaml.YamlField;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public abstract class BaseCreatorService<R extends CreatorResponse, M> {
  public Map<String, YamlField> getInitialDependencies(Map<String, YamlFieldBlob> dependencyBlobs) {
    Map<String, YamlField> initialDependencies = new HashMap<>();

    if (isNotEmpty(dependencyBlobs)) {
      try {
        for (Map.Entry<String, YamlFieldBlob> entry : dependencyBlobs.entrySet()) {
          initialDependencies.put(entry.getKey(), YamlField.fromFieldBlob(entry.getValue()));
        }
      } catch (Exception e) {
        throw new InvalidRequestException("Invalid YAML found in dependency blobs");
      }
    }

    return initialDependencies;
  }

  public R processNodesRecursively(Map<String, YamlField> initialDependencies, M metadata, R finalResponse) {
    if (isEmpty(initialDependencies)) {
      return finalResponse;
    }

    Map<String, YamlField> dependencies = new HashMap<>(initialDependencies);
    while (!dependencies.isEmpty()) {
      processNodes(dependencies, finalResponse, metadata);
      initialDependencies.keySet().forEach(dependencies::remove);
    }

    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies())) {
      initialDependencies.keySet().forEach(k -> finalResponse.getDependencies().remove(k));
    }

    return finalResponse;
  }

  private void processNodes(Map<String, YamlField> dependencies, R finalResponse, M metadata) {
    List<YamlField> dependenciesList = new ArrayList<>(dependencies.values());
    dependencies.clear();

    for (YamlField yamlField : dependenciesList) {
      R response = processNodeInternal(metadata, yamlField);

      if (response == null) {
        finalResponse.addDependency(yamlField);
        continue;
      }
      mergeResponses(finalResponse, response);
      finalResponse.addResolvedDependency(yamlField);
      if (isNotEmpty(response.getDependencies())) {
        response.getDependencies().values().forEach(field -> dependencies.put(field.getNode().getUuid(), field));
      }
    }
  }

  public abstract R processNodeInternal(M metadata, YamlField yamlField);

  public abstract void mergeResponses(R finalResponse, R response);
}
