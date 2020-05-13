package io.harness.utils;

import io.harness.beans.SweepingOutput;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummySweepingOutput implements SweepingOutput {
  String test;
}
