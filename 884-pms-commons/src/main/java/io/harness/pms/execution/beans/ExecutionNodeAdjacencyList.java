package io.harness.pms.execution.beans;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionNodeAdjacencyList {
  List<String> children;
  List<String> nextIds;
}
