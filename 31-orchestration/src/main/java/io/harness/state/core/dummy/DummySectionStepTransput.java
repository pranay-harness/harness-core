package io.harness.state.core.dummy;

import io.harness.beans.SweepingOutput;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class DummySectionStepTransput implements SweepingOutput {
  Map<String, String> map;
}
