package io.harness.utils;

import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;

@Builder
public class DummySectionStepParameters implements StepParameters {
  String childNodeId;
}
