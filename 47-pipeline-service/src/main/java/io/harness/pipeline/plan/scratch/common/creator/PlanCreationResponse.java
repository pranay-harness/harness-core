package io.harness.pipeline.plan.scratch.common.creator;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pipeline.plan.scratch.common.yaml.YamlField;
import io.harness.pipeline.plan.scratch.common.yaml.YamlNode;
import io.harness.plan.PlanNode;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class PlanCreationResponse {
  Map<String, PlanNode> nodes;
  Map<String, YamlField> dependencies;
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
    }

    // TODO: Add logic to update only if newNode has a more recent version.
    nodes.put(newNode.getUuid(), newNode);
    if (dependencies != null) {
      dependencies.remove(newNode.getUuid());
    }
  }

  public void addDependencies(Map<String, YamlField> newNodes) {
    if (EmptyPredicate.isEmpty(newNodes)) {
      return;
    }
    newNodes.values().forEach(this ::addDependency);
  }

  public void addDependency(YamlField field) {
    YamlNode yamlNode = field.getNode();
    if ((dependencies != null && dependencies.containsKey(yamlNode.getUuid()))
        || (nodes != null && nodes.containsKey(yamlNode.getUuid()))) {
      return;
    }

    if (dependencies == null) {
      dependencies = new HashMap<>();
    }
    dependencies.put(yamlNode.getUuid(), field);
  }

  public void mergeStartingNodeId(String otherStartingNodeId) {
    if (startingNodeId == null) {
      startingNodeId = otherStartingNodeId;
      return;
    }
    if (otherStartingNodeId == null) {
      return;
    }
    if (!startingNodeId.equals(otherStartingNodeId)) {
      throw new InvalidRequestException(
          format("Received different set of starting nodes: %s and %s", startingNodeId, otherStartingNodeId));
    }
  }
}
