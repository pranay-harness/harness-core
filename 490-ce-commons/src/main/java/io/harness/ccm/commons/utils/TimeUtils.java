/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.utils;

import static io.harness.ccm.commons.constants.Constants.TIME_ZONE;
import static io.harness.ccm.commons.constants.Constants.ZONE_OFFSET;

import com.google.inject.Singleton;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Calendar;
import lombok.NonNull;

@Singleton
public class TimeUtils {
  public Calendar getDefaultCalendar() {
    return Calendar.getInstance(TIME_ZONE);
  }

  public static Instant toInstant(OffsetDateTime offsetDateTime) {
    return offsetDateTime.toInstant();
  }

  public static Instant toInstant(long epocMilli) {
    return Instant.ofEpochMilli(epocMilli);
  }

  public static OffsetDateTime toOffsetDateTime(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZONE_OFFSET);
  }

  public static OffsetDateTime toOffsetDateTime(long epocMilli) {
    return toOffsetDateTime(toInstant(epocMilli));
  }

  public static long toEpocMilli(OffsetDateTime offsetDateTime) {
    return toEpocMilli(offsetDateTime.toInstant());
  }

  public static long toEpocMilli(Instant instant) {
    return instant.toEpochMilli();
  }

  @NonNull
  public static OffsetDateTime offsetDateTimeNow() {
    return OffsetDateTime.now(ZONE_OFFSET);
  }
}
