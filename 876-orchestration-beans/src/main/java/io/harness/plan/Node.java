package io.harness.plan;

import io.harness.persistence.UuidAccess;

public interface Node extends UuidAccess {
  NodeType getNodeType();
}
