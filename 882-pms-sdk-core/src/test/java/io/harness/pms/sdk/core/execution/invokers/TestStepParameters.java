package io.harness.pms.sdk.core.execution.invokers;

import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("testStepParameters25")
public class TestStepParameters implements StepParameters {
  String param;
}
