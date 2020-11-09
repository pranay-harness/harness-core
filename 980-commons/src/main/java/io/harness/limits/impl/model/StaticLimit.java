package io.harness.limits.impl.model;

import io.harness.limits.lib.LimitType;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class StaticLimit implements io.harness.limits.lib.StaticLimit {
  private int count;
  private final LimitType limitType = LimitType.STATIC;

  public static StaticLimit copy(io.harness.limits.lib.StaticLimit limit) {
    return new StaticLimit(limit.getCount());
  }

  // for morphia
  private StaticLimit() {
    this.count = 0;
  }
}
