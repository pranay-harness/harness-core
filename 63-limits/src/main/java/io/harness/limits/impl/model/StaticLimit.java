package io.harness.limits.impl.model;

import io.harness.limits.lib.LimitType;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class StaticLimit implements io.harness.limits.lib.StaticLimit {
  private int count;
  private final LimitType limitType = LimitType.STATIC;

  // for morphia
  private StaticLimit() {
    this.count = 0;
  }
}
