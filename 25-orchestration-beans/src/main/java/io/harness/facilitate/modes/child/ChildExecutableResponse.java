package io.harness.facilitate.modes.child;

import io.harness.annotations.Redesign;
import io.harness.state.io.StateTransput;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class ChildExecutableResponse {
  String childNodeId;
  List<StateTransput> additionalInputs;
}
