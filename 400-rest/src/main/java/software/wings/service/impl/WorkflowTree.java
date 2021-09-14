/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.beans.ExecutionStatus;
import io.harness.cache.Distributable;
import io.harness.cache.Ordinal;

import software.wings.beans.GraphNode;

import java.io.ObjectStreamClass;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkflowTree implements Distributable, Ordinal {
  public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(WorkflowTree.class).getSerialVersionUID();

  private long contextOrder;
  private String key;
  private List<String> params;

  private ExecutionStatus overrideStatus;
  private GraphNode graph;
  private long lastUpdatedAt = System.currentTimeMillis();

  private boolean wasInvalidated;

  @Override
  public long structureHash() {
    return STRUCTURE_HASH;
  }

  @Override
  public long algorithmId() {
    return GraphRenderer.algorithmId;
  }

  @Override
  public long contextOrder() {
    return contextOrder;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public List<String> parameters() {
    return params;
  }
}
