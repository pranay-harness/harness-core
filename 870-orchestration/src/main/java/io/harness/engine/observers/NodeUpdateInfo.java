package io.harness.engine.observers;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeUpdateInfo {
  String planExecutionId;
  String nodeExecutionId;
}
