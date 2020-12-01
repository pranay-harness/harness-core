package io.harness.utils;

import io.harness.pms.sdk.core.data.Outcome;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummyOutcome implements Outcome {
  String test;
}
