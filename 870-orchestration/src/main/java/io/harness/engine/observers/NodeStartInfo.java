package io.harness.engine.observers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class NodeStartInfo {
  NodeExecution nodeExecution;
  @Builder.Default long updatedTs = System.currentTimeMillis();
}
