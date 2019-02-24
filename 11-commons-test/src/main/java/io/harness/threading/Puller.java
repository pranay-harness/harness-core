package io.harness.threading;

import io.harness.exception.PullTimeoutException;
import org.eclipse.jgit.util.time.MonotonicSystemClock;
import org.eclipse.jgit.util.time.ProposedTimestamp;

import java.time.Duration;

public class Puller {
  public static final MonotonicSystemClock monotonicSystemClock = new MonotonicSystemClock();

  public static long monotonicTimestamp() {
    try (ProposedTimestamp timestamp = monotonicSystemClock.propose()) {
      return timestamp.millis();
    }
  }

  public interface Predicate { boolean condition(); }

  public static void pullFor(Duration timeout, Duration interval, Predicate predicate) {
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
    throw new PullTimeoutException(timeout);
  }
}
