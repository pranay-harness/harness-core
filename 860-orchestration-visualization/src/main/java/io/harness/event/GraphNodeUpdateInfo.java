package io.harness.event;

import io.harness.execution.NodeExecution;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GraphNodeUpdateInfo {
  String planExecutionId;
  List<NodeExecution> nodeExecutions;
}
