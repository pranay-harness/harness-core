package io.harness.facilitate.modes.child;

import io.harness.plan.ExecutionNode;
import io.harness.state.io.StateInput;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ChildExecutableResponse {
  ExecutionNode childNode;
  List<StateInput> additionalInputs;
}
