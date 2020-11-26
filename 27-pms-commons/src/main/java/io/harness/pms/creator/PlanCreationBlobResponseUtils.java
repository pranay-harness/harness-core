package io.harness.pms.creator;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.plan.PlanCreationBlobResponse;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.plan.YamlFieldBlob;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreationBlobResponseUtils {
  public void merge(PlanCreationBlobResponse.Builder builder, PlanCreationBlobResponse other) {
    if (other == null) {
      return;
    }
    addNodes(builder, other.getNodesMap());
    addDependencies(builder, other.getDependenciesMap());
    mergeStartingNodeId(builder, other.getStartingNodeId());
  }

  public void addNodes(PlanCreationBlobResponse.Builder builder, Map<String, PlanNodeProto> newNodes) {
    if (EmptyPredicate.isEmpty(newNodes)) {
      return;
    }
    newNodes.values().forEach(newNode -> addNode(builder, newNode));
  }

  public void addNode(PlanCreationBlobResponse.Builder builder, PlanNodeProto newNode) {
    // TODO: Add logic to update only if newNode has a more recent version.
    builder.putNodes(newNode.getUuid(), newNode);
    builder.removeDependencies(newNode.getUuid());
  }

  public void addDependencies(PlanCreationBlobResponse.Builder builder, Map<String, YamlFieldBlob> fieldBlobs) {
    if (EmptyPredicate.isEmpty(fieldBlobs)) {
      return;
    }
    fieldBlobs.forEach((key, value) -> addDependency(builder, key, value));
  }

  public void addDependency(PlanCreationBlobResponse.Builder builder, String nodeId, YamlFieldBlob fieldBlob) {
    if (builder.containsNodes(nodeId)) {
      return;
    }

    builder.putDependencies(nodeId, fieldBlob);
  }

  public void mergeStartingNodeId(PlanCreationBlobResponse.Builder builder, String otherStartingNodeId) {
    if (EmptyPredicate.isEmpty(otherStartingNodeId)) {
      return;
    }
    if (EmptyPredicate.isEmpty(builder.getStartingNodeId())) {
      builder.setStartingNodeId(otherStartingNodeId);
      return;
    }
    if (!builder.getStartingNodeId().equals(otherStartingNodeId)) {
      throw new InvalidRequestException(String.format(
          "Received different set of starting nodes: %s and %s", builder.getStartingNodeId(), otherStartingNodeId));
    }
  }
}
