package io.harness.dto;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrchestrationGraph {
  String planExecutionId;
  Long startTs;
  Long endTs;
  Status status;

  String rootNodeId;
  OrchestrationAdjacencyList adjacencyList;
}
