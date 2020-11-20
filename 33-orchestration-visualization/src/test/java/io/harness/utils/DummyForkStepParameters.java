package io.harness.utils;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public class DummyForkStepParameters implements StepParameters {
  @Singular List<String> parallelNodeIds;
}
