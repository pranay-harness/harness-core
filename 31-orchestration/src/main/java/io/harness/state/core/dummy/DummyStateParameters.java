package io.harness.state.core.dummy;

import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummyStateParameters implements StateParameters {
  String logExpression;
}
