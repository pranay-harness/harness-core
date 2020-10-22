package io.harness.pipeline.plan.scratch.common.creator;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pms.plan.PlanCreationBlobResponse;
import io.harness.pms.plan.PlanNode;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class PlanCreationResponse {
  @Singular Map<String, PlanNode> nodes;
  @Singular Map<String, YamlField> dependencies;
  String startingNodeId;

  public void merge(PlanCreationResponse other) {
    addNodes(other.getNodes());
    addDependencies(other.getDependencies());
    mergeStartingNodeId(other.getStartingNodeId());
  }

  public void addNodes(Map<String, PlanNode> newNodes) {
    if (EmptyPredicate.isEmpty(newNodes)) {
      return;
    }
    newNodes.values().forEach(this ::addNode);
  }

  public void addNode(PlanNode newNode) {
    if (nodes == null) {
      nodes = new HashMap<>();
    } else if (!(nodes instanceof HashMap)) {
      nodes = new HashMap<>(nodes);
    }

    // TODO: Add logic to update only if newNode has a more recent version.
    nodes.put(newNode.getUuid(), newNode);
    if (dependencies != null) {
      dependencies.remove(newNode.getUuid());
    }
  }

  public void addDependencies(Map<String, YamlField> fields) {
    if (EmptyPredicate.isEmpty(fields)) {
      return;
    }
    fields.values().forEach(this ::addDependency);
  }

  public void addDependency(YamlField field) {
    String nodeId = field.getNode().getUuid();
    if ((dependencies != null && dependencies.containsKey(nodeId)) || (nodes != null && nodes.containsKey(nodeId))) {
      return;
    }

    if (dependencies == null) {
      dependencies = new HashMap<>();
    } else if (!(dependencies instanceof HashMap)) {
      dependencies = new HashMap<>(dependencies);
    }
    dependencies.put(nodeId, field);
  }

  public void mergeStartingNodeId(String otherStartingNodeId) {
    if (EmptyPredicate.isEmpty(otherStartingNodeId)) {
      return;
    }
    if (EmptyPredicate.isEmpty(startingNodeId)) {
      startingNodeId = otherStartingNodeId;
      return;
    }
    if (!startingNodeId.equals(otherStartingNodeId)) {
      throw new InvalidRequestException(
          String.format("Received different set of starting nodes: %s and %s", startingNodeId, otherStartingNodeId));
    }
  }

  public PlanCreationBlobResponse toBlobResponse() {
    PlanCreationBlobResponse.Builder finalBlobResponseBuilder = PlanCreationBlobResponse.newBuilder();
    if (EmptyPredicate.isNotEmpty(nodes)) {
      finalBlobResponseBuilder.putAllNodes(nodes);
    }
    if (EmptyPredicate.isNotEmpty(dependencies)) {
      for (Map.Entry<String, YamlField> dependency : dependencies.entrySet()) {
        finalBlobResponseBuilder.putDependencies(dependency.getKey(), dependency.getValue().toFieldBlob());
      }
    }
    if (EmptyPredicate.isNotEmpty(startingNodeId)) {
      finalBlobResponseBuilder.setStartingNodeId(startingNodeId);
    }
    return finalBlobResponseBuilder.build();
  }
}
