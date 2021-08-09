package io.harness.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class DateTimeUtils {
  public static final String UTC_TIMEZONE = "UTC";

  // DO NOT CHANGE THESE
  public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  public String formatDate(TemporalAccessor date) {
    return DATE_FORMATTER.format(date);
  }

  public String formatDateTime(TemporalAccessor date) {
    return DATETIME_FORMATTER.format(date);
  }
}
