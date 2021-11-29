package io.harness.pms.sdk.core.pipeline.creators;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.Dependencies;
import io.harness.pms.yaml.YamlField;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface CreatorResponse {
  Dependencies getDependenciesForVariable();
  Map<String, YamlField> getDependencies();
  void addDependency(YamlField field);
  void addDependency(String yaml, String nodeId, String yamlPath);
  void addResolvedDependency(YamlField yamlField);
  void addResolvedDependency(String yaml, String nodeId, String yamlPath);
}
