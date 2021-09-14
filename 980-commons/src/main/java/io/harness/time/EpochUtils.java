/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EpochUtils {
  public static final String PST_ZONE_ID = "America/Los_Angeles";

  public static long calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(int days, String zoneId) {
    return LocalDate.now(ZoneId.of(zoneId))
        .minus(days - 1L, ChronoUnit.DAYS)
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
