package io.harness.limits.impl.model;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.lib.LimitType;
import io.harness.limits.lib.RateBasedLimit;

import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Value;

@OwnedBy(PL)
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
