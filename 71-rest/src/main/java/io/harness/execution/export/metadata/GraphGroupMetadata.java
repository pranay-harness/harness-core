package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class GraphGroupMetadata implements GraphNodeVisitable {
  List<List<GraphNodeMetadata>> elements;
  ExecutionStrategy executionStrategy;

  public void accept(GraphNodeVisitor visitor) {
    if (isEmpty(elements)) {
      return;
    }

    for (List<GraphNodeMetadata> element : elements) {
      MetadataUtils.acceptMultiple(visitor, element);
    }
  }

  static GraphGroupMetadata fromGraphGroup(GraphGroup graphGroup) {
    if (graphGroup == null || isEmpty(graphGroup.getElements())) {
      return null;
    }

    List<List<GraphNodeMetadata>> elements = new ArrayList<>();
    for (GraphNode node : graphGroup.getElements()) {
      List<GraphNodeMetadata> currElement = GraphNodeMetadata.fromOriginGraphNode(node);
      if (isNotEmpty(currElement)) {
        elements.add(currElement);
      }
    }

    return GraphGroupMetadata.builder().elements(elements).executionStrategy(graphGroup.getExecutionStrategy()).build();
  }
}
