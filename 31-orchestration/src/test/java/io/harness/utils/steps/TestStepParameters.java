package io.harness.utils.steps;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestStepParameters implements StepParameters {
  String param;
}
