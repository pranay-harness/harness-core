package io.harness.utils;

import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;

@Builder
public class DummyForkStepParameters implements StepParameters {
  @Singular List<String> parallelNodeIds;
}
