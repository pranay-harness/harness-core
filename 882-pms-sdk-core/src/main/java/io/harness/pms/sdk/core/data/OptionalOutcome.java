package io.harness.pms.sdk.core.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionalOutcome {
  boolean found;
  Outcome outcome;
}
