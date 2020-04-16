package io.harness.facilitate.modes.children;

import io.harness.annotations.Redesign;
import io.harness.state.io.StateTransput;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class ChildrenExecutableResponse {
  @Singular List<Child> children;

  @Value
  @Builder
  public static class Child {
    String childNodeId;
    List<StateTransput> additionalInputs;
  }
}
