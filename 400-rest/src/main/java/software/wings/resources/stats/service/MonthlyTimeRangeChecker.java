/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.service;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.resources.stats.model.TimeRange;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.commons.lang3.Range;

@OwnedBy(HarnessTeam.CDC)
public class MonthlyTimeRangeChecker implements TimeRangeChecker {
  @Override
  public boolean istTimeInRange(TimeRange timeRange, long currentTimeMillis) {
    if (isRecurrentRangeExpired(timeRange, currentTimeMillis)) {
      return false;
    }
    LocalDateTime startDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeRange.getFrom()), ZoneId.of(timeRange.getTimeZone()));
    int startMonth = startDateTime.getMonthValue();
    int startDayOfMonth = startDateTime.getDayOfMonth();
    int maxDaysInMonth = (int) startDateTime.range(DAY_OF_MONTH).getMaximum();

    LocalDateTime endDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeRange.getTo()), ZoneId.of(timeRange.getTimeZone()));
    int endMonth = endDateTime.getMonthValue();
    int endDayOfMonth = endDateTime.getDayOfMonth();

    LocalDateTime currentDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.of(timeRange.getTimeZone()));
    int currentDayOfMonth = currentDateTime.getDayOfMonth();

    if (!matchesDayOfMonth(startDayOfMonth, endDayOfMonth, currentDayOfMonth, maxDaysInMonth)) {
      return false;
    }

    return isMatchedDayInRangeExactly(startDateTime, startMonth, startDayOfMonth, endDateTime, endMonth, endDayOfMonth,
        currentDateTime, currentDayOfMonth);
  }

  private boolean isRecurrentRangeExpired(TimeRange timeRange, long currentTimeMillis) {
    return timeRange.getEndTime() <= currentTimeMillis;
  }

  private boolean isMatchedDayInRangeExactly(LocalDateTime startDateTime, int startMonth, int startDayOfMonth,
      LocalDateTime endDateTime, int endMonth, int endDayOfMonth, LocalDateTime currentDateTime,
      int currentDayOfMonth) {
    if (startMonth == endMonth && startDayOfMonth == endDayOfMonth) {
      return startDateTime.toLocalTime().isBefore(currentDateTime.toLocalTime())
          && endDateTime.toLocalTime().isAfter(currentDateTime.toLocalTime());
    } else if (startDayOfMonth == currentDayOfMonth) {
      return startDateTime.toLocalTime().isBefore(currentDateTime.toLocalTime());
    } else if (endDayOfMonth == currentDayOfMonth) {
      return endDateTime.toLocalTime().isAfter(currentDateTime.toLocalTime());
    } else {
      return true;
    }
  }

  private boolean matchesDayOfMonth(int startDayOfMonth, int endDayOfMonth, int currentDayOfMonth, int maxDaysInMonth) {
    if (startDayOfMonth == endDayOfMonth && currentDayOfMonth == startDayOfMonth) {
      return true;
    }
    if (startDayOfMonth > endDayOfMonth) {
      return Range.between(startDayOfMonth, maxDaysInMonth).contains(currentDayOfMonth)
          || Range.between(1, endDayOfMonth).contains(currentDayOfMonth);
    } else if (startDayOfMonth < endDayOfMonth) {
      return Range.between(startDayOfMonth, endDayOfMonth).contains(currentDayOfMonth);
    }
    return false;
  }
}
