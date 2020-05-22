package io.harness.utils;

import io.harness.state.io.StepParameters;
import lombok.Builder;

@Builder
public class TestStepParameters implements StepParameters {
  String param;
}
