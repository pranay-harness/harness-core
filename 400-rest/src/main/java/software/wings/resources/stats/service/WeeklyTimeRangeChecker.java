/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.resources.stats.model.TimeRange;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.commons.lang3.Range;

@OwnedBy(HarnessTeam.CDC)
public class WeeklyTimeRangeChecker implements TimeRangeChecker {
  @Override
  public boolean istTimeInRange(TimeRange timeRange, long currentTimeMillis) {
    if (isRecurrentRangeExpired(timeRange, currentTimeMillis)) {
      return false;
    }
    LocalDateTime startDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeRange.getFrom()), ZoneId.of(timeRange.getTimeZone()));
    int startDayOfWeek = startDateTime.getDayOfWeek().getValue();

    LocalDateTime endDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeRange.getTo()), ZoneId.of(timeRange.getTimeZone()));
    int endDayOfWeek = endDateTime.getDayOfWeek().getValue();

    LocalDateTime currentDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.of(timeRange.getTimeZone()));
    int currentDayOfWeek = currentDateTime.getDayOfWeek().getValue();

    if (!matchesDayOfWeek(startDayOfWeek, endDayOfWeek, currentDayOfWeek)) {
      return false;
    }

    if (startDayOfWeek == endDayOfWeek) {
      return startDateTime.toLocalTime().isBefore(currentDateTime.toLocalTime())
          && endDateTime.toLocalTime().isAfter(currentDateTime.toLocalTime());
    } else if (startDayOfWeek == currentDayOfWeek) {
      return startDateTime.toLocalTime().isBefore(currentDateTime.toLocalTime());
    } else if (endDayOfWeek == currentDayOfWeek) {
      return endDateTime.toLocalTime().isAfter(currentDateTime.toLocalTime());
    } else {
      return true;
    }
  }

  private boolean isRecurrentRangeExpired(TimeRange timeRange, long currentTimeMillis) {
    return timeRange.getEndTime() <= currentTimeMillis;
  }

  private boolean matchesDayOfWeek(int startDayOfWeek, int endDayOfWeek, int currentDayOfWeek) {
    if (startDayOfWeek == endDayOfWeek && currentDayOfWeek == startDayOfWeek) {
      return true;
    }
    if (startDayOfWeek > endDayOfWeek) {
      return Range.between(startDayOfWeek, 7).contains(currentDayOfWeek)
          || Range.between(1, endDayOfWeek).contains(currentDayOfWeek);
    } else if (startDayOfWeek < endDayOfWeek) {
      return Range.between(startDayOfWeek, endDayOfWeek).contains(currentDayOfWeek);
    }
    return false;
  }
}
