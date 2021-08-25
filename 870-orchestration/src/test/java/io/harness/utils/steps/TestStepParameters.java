package io.harness.utils.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("testStepParameters25")
@RecasterAlias("io.harness.utils.steps.TestStepParameters")
public class TestStepParameters implements StepParameters {
  String param;
}
