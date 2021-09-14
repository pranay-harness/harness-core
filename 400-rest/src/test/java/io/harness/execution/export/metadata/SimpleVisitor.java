/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.execution.export.metadata;

import java.util.ArrayList;
import java.util.List;

public class SimpleVisitor implements GraphNodeVisitor {
  private final List<String> visited = new ArrayList<>();

  public void visitGraphNode(GraphNodeMetadata nodeMetadata) {
    if (nodeMetadata.getId() == null) {
      throw new RuntimeException("invalid id");
    }

    visited.add(nodeMetadata.getId());
  }

  public List<String> getVisited() {
    return visited;
  }
}
