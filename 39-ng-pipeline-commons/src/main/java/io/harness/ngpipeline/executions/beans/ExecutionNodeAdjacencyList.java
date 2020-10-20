package io.harness.ngpipeline.executions.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ExecutionNodeAdjacencyList {
  List<String> children;
  List<String> nextIds;
}
