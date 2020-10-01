package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class EphemeralOrchestrationGraph {
  String planExecutionId;
  Long startTs;
  Long endTs;
  Status status;

  List<String> rootNodeIds;
  OrchestrationAdjacencyListInternal adjacencyList;
}
