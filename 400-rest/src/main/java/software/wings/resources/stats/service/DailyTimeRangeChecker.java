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
import java.time.LocalTime;
import java.time.ZoneId;

@OwnedBy(HarnessTeam.CDC)
public class DailyTimeRangeChecker implements TimeRangeChecker {
  @Override
  public boolean istTimeInRange(TimeRange timeRange, long currentTimeMillis) {
    if (isRecurrentRangeExpired(timeRange, currentTimeMillis)) {
      return false;
    }
    LocalTime startTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeRange.getFrom()), ZoneId.of(timeRange.getTimeZone()))
            .toLocalTime();
    LocalTime endTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timeRange.getTo()), ZoneId.of(timeRange.getTimeZone()))
            .toLocalTime();

    LocalTime currentTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.of(timeRange.getTimeZone()))
            .toLocalTime();

    return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
  }

  private boolean isRecurrentRangeExpired(TimeRange timeRange, long currentTimeMillis) {
    return timeRange.getEndTime() <= currentTimeMillis;
  }
}
