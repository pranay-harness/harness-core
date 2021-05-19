package io.harness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX)
public class NGDateUtils {
  public static final long HOUR_IN_MS = 60 * 60 * 1000;
  public static final long DAY_IN_MS = 24 * HOUR_IN_MS;
  public static final long MIN_DAYS_IN_YEAR = 365;
  public static final long MIN_DAYS_IN_6MONTHS = 182;
  public static final long MIN_DAYS_IN_MONTH = 28;

  public static long getStartTimeOfTheDayAsEpoch(long epoch) {
    return epoch - epoch % DAY_IN_MS;
  }

  /** 00:00:00 is considered as start time of a day */
  public static long getStartTimeOfNextDay(long epoch) {
    return getStartTimeOfTheDayAsEpoch(epoch) + DAY_IN_MS;
  }

  public static long getNumberOfDays(long start, long end) {
    return (end - start) / DAY_IN_MS + 1;
  }
}
