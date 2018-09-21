package io.harness.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class EpochUtils {
  public static final String PST_ZONE_ID = "America/Los_Angeles";

  public static long calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(int days, String zoneId) {
    return LocalDate.now(ZoneId.of(zoneId))
        .minus(days - 1, ChronoUnit.DAYS)
        .atStartOfDay(ZoneId.of(zoneId))
        .toInstant()
        .toEpochMilli();
  }

  public static long obtainStartOfTheDayEpoch(long epoch, String zoneId) {
    return Instant.ofEpochMilli(epoch)
        .atZone(ZoneId.of(zoneId))
        .toLocalDate()
        .atStartOfDay(ZoneId.of(zoneId))
        .toInstant()
        .toEpochMilli();
  }
}
