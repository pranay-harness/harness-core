package io.harness.state.core.dummy;

import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class DummySectionOutcome implements Outcome {
  Map<String, String> map;
}
