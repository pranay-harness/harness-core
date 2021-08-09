package io.harness.pms.sdk.core.execution;

import io.harness.pms.sdk.core.steps.io.PassThroughData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChainDetails {
  boolean shouldEnd;
  PassThroughData passThroughData;
}
