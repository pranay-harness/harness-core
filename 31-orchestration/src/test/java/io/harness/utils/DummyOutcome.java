package io.harness.utils;

import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummyOutcome implements Outcome {
  String test;
}
