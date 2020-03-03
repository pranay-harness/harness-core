package io.harness.flow;

import com.google.common.util.concurrent.AbstractScheduledService.CustomScheduler;

import lombok.Getter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CustomScheduler} that increases delay exponentially on failure.
 */
public class BackoffScheduler extends CustomScheduler {
  private final long minDelayMs;
  private final long maxDelayMs;

  @Getter private long currentDelayMs;

  public BackoffScheduler(Duration minDelay, Duration maxDelay) {
    this.minDelayMs = minDelay.toMillis();
    this.maxDelayMs = maxDelay.toMillis();
    this.currentDelayMs = minDelayMs;
  }

  public void recordSuccess() {
    currentDelayMs = Math.max(minDelayMs, currentDelayMs / 2);
  }

  public void recordFailure() {
    currentDelayMs = Math.min(maxDelayMs, 2 * currentDelayMs);
  }

  @Override
  protected Schedule getNextSchedule() {
    return new Schedule(currentDelayMs, TimeUnit.MILLISECONDS);
  }
}
