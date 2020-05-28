package io.harness.engine.interrupts;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterruptCheck {
  boolean proceed;
  String reason;
}
