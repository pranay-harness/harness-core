package io.harness.steps.common;

import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("ngSectionStepParameters")
public class NGSectionStepParameters implements StepParameters {
  String childNodeId;
  String logMessage;
}
