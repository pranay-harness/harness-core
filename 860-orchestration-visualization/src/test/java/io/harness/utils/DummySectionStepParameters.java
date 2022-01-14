package io.harness.utils;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;

@Builder
@RecasterAlias("io.harness.utils.DummySectionStepParameters")
public class DummySectionStepParameters implements StepParameters {
  String childNodeId;
}
