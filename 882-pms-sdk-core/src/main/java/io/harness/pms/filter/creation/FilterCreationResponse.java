package io.harness.pms.filter.creation;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.yaml.YamlField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

@Data
@Builder
public class FilterCreationResponse {
  PipelineFilter pipelineFilter;
  int stageCount;
  @Default Map<String, YamlField> dependencies = new HashMap<>();
  @Default Map<String, YamlField> resolvedDependencies = new HashMap<>();
  @Default List<EntityDetailProtoDTO> referredEntities = new ArrayList<>();
  @Default List<String> stageNames = new ArrayList<>();

  public void addResolvedDependencies(Map<String, YamlField> resolvedDependencies) {
    if (hasNone(resolvedDependencies)) {
      return;
    }
    resolvedDependencies.values().forEach(this::addResolvedDependency);
  }

  public void addReferredEntities(List<EntityDetailProtoDTO> refferedEntities) {
    if (hasNone(refferedEntities)) {
      return;
    }
    if (hasNone(this.referredEntities)) {
      this.referredEntities = new ArrayList<>();
    }
    this.referredEntities.addAll(refferedEntities);
  }

  public void addStageNames(List<String> stageNames) {
    if (hasNone(stageNames)) {
      return;
    }
    if (hasNone(this.stageNames)) {
      this.stageNames = new ArrayList<>();
    }
    this.stageNames.addAll(stageNames);
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
    if (hasNone(fields)) {
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

  public FilterCreationBlobResponse toBlobResponse() {
    FilterCreationBlobResponse.Builder finalBlobResponseBuilder = FilterCreationBlobResponse.newBuilder();
    if (pipelineFilter != null) {
      finalBlobResponseBuilder.setFilter(pipelineFilter.toJson());
    }

    if (hasSome(dependencies)) {
      for (Map.Entry<String, YamlField> dependency : dependencies.entrySet()) {
        finalBlobResponseBuilder.putDependencies(dependency.getKey(), dependency.getValue().toFieldBlob());
      }
    }

    if (hasSome(resolvedDependencies)) {
      for (Map.Entry<String, YamlField> dependency : resolvedDependencies.entrySet()) {
        finalBlobResponseBuilder.putResolvedDependencies(dependency.getKey(), dependency.getValue().toFieldBlob());
      }
    }

    if (hasSome(referredEntities)) {
      finalBlobResponseBuilder.addAllReferredEntities(referredEntities);
    }

    if (hasSome(stageNames)) {
      finalBlobResponseBuilder.addAllStageNames(stageNames);
    }

    finalBlobResponseBuilder.setStageCount(stageCount);
    return finalBlobResponseBuilder.build();
  }
}
