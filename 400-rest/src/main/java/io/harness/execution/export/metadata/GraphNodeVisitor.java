package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface GraphNodeVisitor {
  void visitGraphNode(GraphNodeMetadata nodeMetadata);
}
