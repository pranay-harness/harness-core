package io.harness.limits.impl.model;

import io.harness.limits.lib.LimitType;
import io.harness.limits.lib.RateBasedLimit;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

@Value
@AllArgsConstructor
public class RateLimit implements RateBasedLimit {
  private int count;
  private int duration;
  @NotNull private TimeUnit durationUnit;
  private final LimitType limitType = LimitType.RATE_LIMIT;

  // for morphia
  private RateLimit() {
    this.count = 0;
    this.duration = 0;
    this.durationUnit = null;
  }
}
