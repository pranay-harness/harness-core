/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.threading;

import io.harness.exception.PollTimeoutException;

import java.time.Duration;
import org.eclipse.jgit.util.time.MonotonicSystemClock;
import org.eclipse.jgit.util.time.ProposedTimestamp;

public class Poller {
  public static final MonotonicSystemClock monotonicSystemClock = new MonotonicSystemClock();

  public static long monotonicTimestamp() {
    try (ProposedTimestamp timestamp = monotonicSystemClock.propose()) {
      return timestamp.millis();
    }
  }

  public interface Predicate {
    boolean condition();
  }

  public static void pollFor(Duration timeout, Duration interval, Predicate predicate) {
    long start = monotonicTimestamp();
    do {
      if (predicate.condition()) {
        return;
      }
      try {
        Thread.sleep(interval.toMillis());
      } catch (InterruptedException e) {
        // Do nothing
      }
    } while (monotonicTimestamp() - start < timeout.toMillis());
    throw new PollTimeoutException(timeout);
  }
}
