package io.harness.pms.sdk.core.pipeline.creators;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.yaml.YamlField;

import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
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

  public R processNodesRecursively(Dependencies initialDependencies, M metadata, R finalResponse) {
    if (isEmpty(initialDependencies.getDependenciesMap())) {
      return finalResponse;
    }

    Dependencies.Builder dependencies = Dependencies.newBuilder()
                                            .setYaml(initialDependencies.getYaml())
                                            .putAllDependencies(initialDependencies.getDependenciesMap());
    while (!dependencies.getDependenciesMap().isEmpty()) {
      processNodes(dependencies, finalResponse, metadata);
      for (Map.Entry<String, String> entry : initialDependencies.getDependenciesMap().entrySet()) {
        dependencies.removeDependencies(entry.getKey());
      }
    }

    if (EmptyPredicate.isNotEmpty(finalResponse.getDependencies().getDependenciesMap())) {
      for (Map.Entry<String, String> entry : initialDependencies.getDependenciesMap().entrySet()) {
        finalResponse.getDependencies().toBuilder().removeDependencies(entry.getKey()).build();
      }
    }

    return finalResponse;
  }

  private void processNodes(Dependencies.Builder dependencies, R finalResponse, M metadata) {
    List<String> yamlPathList = new ArrayList<>(dependencies.getDependenciesMap().values());
    String currentYaml = dependencies.getYaml();
    dependencies.clearDependencies();

    for (String yamlPath : yamlPathList) {
      YamlField yamlField = null;
      try {
        yamlField = YamlField.fromYamlPath(currentYaml, yamlPath);
      } catch (IOException e) {
        log.error("Invalid yaml field", e);
      }
      R response = processNodeInternal(metadata, yamlField);

      if (response == null) {
        finalResponse.addDependency(currentYaml, yamlField.getNode().getUuid(), yamlPath);
        continue;
      }
      mergeResponses(finalResponse, response);
      finalResponse.addResolvedDependency(currentYaml, yamlField.getNode().getUuid(), yamlPath);
      if (isNotEmpty(response.getDependencies().getDependenciesMap())) {
        for (Map.Entry<String, String> entry : response.getDependencies().getDependenciesMap().entrySet()) {
          dependencies.putDependencies(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public abstract R processNodeInternal(M metadata, YamlField yamlField);

  public abstract void mergeResponses(R finalResponse, R response);
}
