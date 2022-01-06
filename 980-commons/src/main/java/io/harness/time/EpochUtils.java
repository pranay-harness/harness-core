/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
