package io.harness.cvng.core.utils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public class DateTimeUtils {
  private DateTimeUtils() {}

  public static Instant roundDownTo5MinBoundary(Instant instant) {
    ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.UTC);
    int minute = zonedDateTime.getMinute();
    minute = minute - minute % 5;
    zonedDateTime = ZonedDateTime.of(zonedDateTime.getYear(), zonedDateTime.getMonthValue(),
        zonedDateTime.getDayOfMonth(), zonedDateTime.getHour(), minute, 0, 0, ZoneOffset.UTC);
    return zonedDateTime.toInstant();
  }

  public static long instantToEpochMinute(Instant instant) {
    return TimeUnit.MILLISECONDS.toMinutes(instant.toEpochMilli());
  }
}
