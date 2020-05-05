package io.harness.facilitator.children;

import io.harness.state.io.StateParameters;
import lombok.Builder;

@Builder
public class TestStateParameters implements StateParameters {
  String param;
}
