/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.service;

import static io.harness.governance.TimeRangeOccurrence.MONTHLY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.resources.stats.model.TimeRange;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.commons.lang3.Range;

@OwnedBy(HarnessTeam.CDC)
public class AnnualTimeRangeChecker implements TimeRangeChecker {
  @Override
  public boolean istTimeInRange(TimeRange timeRange, long currentTimeMillis) {
    if (isRecurrentRangeExpired(timeRange, currentTimeMillis)) {
      return false;
    }
    LocalDateTime startDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeRange.getFrom()), ZoneId.of(timeRange.getTimeZone()));
    int startMonth = startDateTime.getMonthValue();
    int startDayOfMonth = startDateTime.getDayOfMonth();

    LocalDateTime endDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeRange.getTo()), ZoneId.of(timeRange.getTimeZone()));
    int endMonth = endDateTime.getMonthValue();
    int endDayOfMonth = endDateTime.getDayOfMonth();

    LocalDateTime currentDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.of(timeRange.getTimeZone()));
    int currentMonth = currentDateTime.getMonthValue();
    int currentDayOfMonth = currentDateTime.getDayOfMonth();

    if (!matchesMonth(startMonth, endMonth, currentMonth)) {
      return false;
    }

    if (startMonth == endMonth) {
      return MONTHLY.getTimeRangeChecker().istTimeInRange(timeRange, currentTimeMillis);
    }

    if (isMonthStrictlyInRange(startMonth, endMonth, currentMonth)) {
      return true;
    }

    if (currentMonth == startMonth) {
      if (currentDayOfMonth < startDayOfMonth) {
        return false;
      }
      if (currentDayOfMonth > startDayOfMonth) {
        return true;
      }
      return currentDateTime.toLocalTime().isAfter(startDateTime.toLocalTime());
    }

    if (currentMonth == endMonth) {
      if (currentDayOfMonth > endDayOfMonth) {
        return false;
      }
      if (currentDayOfMonth < endDayOfMonth) {
        return true;
      }
      return currentDateTime.toLocalTime().isBefore(endDateTime.toLocalTime());
    }

    return false;
  }

  private boolean isMonthStrictlyInRange(int startMonth, int endMonth, int currentMonth) {
    return currentMonth > startMonth && currentMonth < endMonth;
  }

  private boolean isRecurrentRangeExpired(TimeRange timeRange, long currentTimeMillis) {
    return timeRange.getEndTime() <= currentTimeMillis;
  }

  private boolean matchesMonth(int startMonth, int endMonth, int currentMonth) {
    if (startMonth == endMonth && currentMonth == startMonth) {
      return true;
    }
    if (startMonth > endMonth) {
      return Range.between(startMonth, 12).contains(currentMonth) || Range.between(1, endMonth).contains(currentMonth);
    } else if (startMonth < endMonth) {
      return Range.between(startMonth, endMonth).contains(currentMonth);
    }
    return false;
  }
}
