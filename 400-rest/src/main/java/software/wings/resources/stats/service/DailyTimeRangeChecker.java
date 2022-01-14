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
