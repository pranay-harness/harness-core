package io.harness.state.core.dummy;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummyStepParameters implements StepParameters {
  String logExpression;
}
