package io.harness.engine.pms.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionalOutcome {
  boolean found;
  String outcome;
}
