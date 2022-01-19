package io.harness.engine.observers;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EndObserverResult {
  boolean successful;
}
