package io.harness.pms.filter.creation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.FilterCreationBlobResponse;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

@Data
@Builder
public class FilterCreationResponse {
  PipelineFilter pipelineFilter;
  int stageCount;
  String startingNodeId;
  @Default Map<String, GraphLayoutNode> layoutNodes = new HashMap<>();
  @Default Map<String, YamlField> dependencies = new HashMap<>();
  @Default Map<String, YamlField> resolvedDependencies = new HashMap<>();

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

  public void addLayoutNodes(Map<String, GraphLayoutNode> layoutNodes) {
    if (EmptyPredicate.isEmpty(layoutNodes)) {
      return;
    }
    layoutNodes.values().forEach(this::addLayoutNode);
  }

  public void addLayoutNode(GraphLayoutNode layoutNode) {
    if (layoutNode == null) {
      return;
    }
    if (layoutNodes == null) {
      layoutNodes = new HashMap<>();
    } else if (!(layoutNodes instanceof HashMap)) {
      layoutNodes = new HashMap<>(layoutNodes);
    }
    layoutNodes.put(layoutNode.getNodeUUID(), layoutNode);
  }

  public FilterCreationBlobResponse toBlobResponse() {
    FilterCreationBlobResponse.Builder finalBlobResponseBuilder = FilterCreationBlobResponse.newBuilder();
    if (pipelineFilter != null) {
      finalBlobResponseBuilder.setFilter(pipelineFilter.toJson());
    }

    if (isNotEmpty(dependencies)) {
      for (Map.Entry<String, YamlField> dependency : dependencies.entrySet()) {
        finalBlobResponseBuilder.putDependencies(dependency.getKey(), dependency.getValue().toFieldBlob());
      }
    }

    if (isNotEmpty(resolvedDependencies)) {
      for (Map.Entry<String, YamlField> dependency : resolvedDependencies.entrySet()) {
        finalBlobResponseBuilder.putResolvedDependencies(dependency.getKey(), dependency.getValue().toFieldBlob());
      }
    }

    if (isNotEmpty(layoutNodes)) {
      finalBlobResponseBuilder.putAllLayoutNodes(layoutNodes);
    }

    finalBlobResponseBuilder.setStageCount(stageCount);
    if (isNotEmpty(startingNodeId)) {
      finalBlobResponseBuilder.setStartingNodeId(startingNodeId);
    }
    return finalBlobResponseBuilder.build();
  }
}
