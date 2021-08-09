package io.harness.batch.processing.support;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ActiveInstanceIterator {
  private static int OFFSET_DAYS = 50 * 365;

  public static Instant getActiveInstanceIteratorFromStartTime(Instant startTime) {
    return startTime.plus(OFFSET_DAYS, ChronoUnit.DAYS).plus(getOffsetValue(0, 100), ChronoUnit.MILLIS);
  }

  public static Instant getActiveInstanceIteratorFromStopTime(Instant stopTime) {
    return stopTime.plus(getOffsetValue(0, 100), ChronoUnit.MILLIS);
  }

  public static int getOffsetValue(int min, int max) {
    return (int) ((Math.random() * (max - min)) + min);
  }
}
