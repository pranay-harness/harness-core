package io.harness.state.io;

import io.harness.annotations.Redesign;
import io.harness.state.execution.status.NodeExecutionStatus;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class StateResponse {
  NodeExecutionStatus executionStatus;
  @Singular List<StateTransput> outputs;
  String errorMessage;
}
