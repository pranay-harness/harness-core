/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
