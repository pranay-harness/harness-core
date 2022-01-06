/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
