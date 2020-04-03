package io.harness.facilitate.modes.children;

import io.harness.plan.ExecutionNodeDefinition;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ChildrenExecutableResponse {
  List<ExecutionNodeDefinition> childNodes;
}
