package io.harness.presentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Graph {
  String planExecutionId;
  Long startTs;
  Long endTs;
  Status status;
  GraphVertex graphVertex;
}
