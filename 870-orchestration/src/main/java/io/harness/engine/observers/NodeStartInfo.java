/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.observers;

import io.harness.execution.NodeExecution;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NodeStartInfo {
  NodeExecution nodeExecution;
  @Builder.Default long updatedTs = System.currentTimeMillis();
}
