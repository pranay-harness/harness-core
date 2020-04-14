package io.harness.facilitate.modes.children;

import io.harness.annotations.Redesign;
import io.harness.plan.ExecutionNode;
import io.harness.state.io.StateTransput;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class ChildrenExecutableResponse {
  List<Child> children;

  public static class Child {
    ExecutionNode childNode;
    List<StateTransput> additionalInputs;
  }
}
