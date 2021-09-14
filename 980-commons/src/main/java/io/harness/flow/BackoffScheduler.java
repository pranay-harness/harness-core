/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.flow;

import com.google.common.util.concurrent.AbstractScheduledService.CustomScheduler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link CustomScheduler} that increases delay exponentially on failure.
 */
@Slf4j
public class BackoffScheduler extends CustomScheduler {
  private final String service;
  private final long minDelayMs;
  private final long maxDelayMs;

  @Getter private long currentDelayMs;

  public BackoffScheduler(String service, Duration minDelay, Duration maxDelay) {
    this.service = service;
    this.minDelayMs = minDelay.toMillis();
    this.maxDelayMs = maxDelay.toMillis();
    this.currentDelayMs = minDelayMs;
  }

  public void recordSuccess() {
    long newDelayMs = Math.max(minDelayMs, currentDelayMs / 2);
    if (newDelayMs != currentDelayMs) {
      log.info("{} recover from {}ms to {}ms", service, currentDelayMs, newDelayMs);
      currentDelayMs = newDelayMs;
    }
  }

  public void recordFailure() {
    long newDelayMs = Math.min(maxDelayMs, 2 * currentDelayMs);
    if (newDelayMs != currentDelayMs) {
      log.warn("{} back-off from {}ms to {}ms", service, currentDelayMs, newDelayMs);
      currentDelayMs = newDelayMs;
    }
  }

  @Override
  protected Schedule getNextSchedule() {
    return new Schedule(currentDelayMs, TimeUnit.MILLISECONDS);
  }
}
