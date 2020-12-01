package io.harness.utils;

import io.harness.pms.sdk.core.data.Outcome;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class DummyOutcome implements Outcome {
  String test;
}
