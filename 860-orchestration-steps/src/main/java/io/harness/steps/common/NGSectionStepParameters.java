package io.harness.steps.common;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("ngSectionStepParameters")
@RecasterAlias("io.harness.steps.common.NGSectionStepParameters")
public class NGSectionStepParameters implements StepParameters {
  String childNodeId;
  String logMessage;
}
