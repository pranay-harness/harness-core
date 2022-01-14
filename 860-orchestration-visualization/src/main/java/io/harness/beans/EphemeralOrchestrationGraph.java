package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.pms.contracts.execution.Status;

import java.util.List;
import lombok.Builder;
import lombok.Value;

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
