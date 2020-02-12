package io.harness.event.client.impl.tailer;

import com.google.common.util.concurrent.AbstractScheduledService.CustomScheduler;

import lombok.Getter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

class BackoffScheduler extends CustomScheduler {
  private final long minDelayMs;
  private final long maxDelayMs;

  @Getter private long currentDelayMs;

  BackoffScheduler(Duration minDelay, Duration maxDelay) {
    this.minDelayMs = minDelay.toMillis();
    this.maxDelayMs = maxDelay.toMillis();
    this.currentDelayMs = minDelayMs;
  }

  void recordSuccess() {
    currentDelayMs = Math.max(minDelayMs, currentDelayMs / 2);
  }

  void recordFailure() {
    currentDelayMs = Math.min(maxDelayMs, 2 * currentDelayMs);
  }

  @Override
  protected Schedule getNextSchedule() {
    return new Schedule(currentDelayMs, TimeUnit.MILLISECONDS);
  }
}
