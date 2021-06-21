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
