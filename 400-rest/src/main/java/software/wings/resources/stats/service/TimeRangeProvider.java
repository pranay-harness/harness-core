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
import java.time.Month;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@ParametersAreNonnullByDefault
@OwnedBy(HarnessTeam.CDP)
public class TimeRangeProvider {
  private static final ZoneOffset DEFAULT_ZONE = ZoneOffset.UTC;

  private ZoneOffset zone = DEFAULT_ZONE;
  public TimeRangeProvider(ZoneOffset zone) {
    this.zone = zone;
  }

  public List<TimeRange> monthlyRanges(Instant from, Instant to) {
    List<TimeRange> ranges = new LinkedList<>();
    LocalDateTime fromTime = LocalDateTime.ofInstant(from, zone);
    LocalDateTime toTime = LocalDateTime.ofInstant(to, zone);

    TimeRange range = month(fromTime);
    ranges.add(range);

    LocalDateTime time = fromTime.with(TemporalAdjusters.firstDayOfNextMonth());
    while (!time.isAfter(toTime)) {
      ranges.add(month(time));
      time = time.with(TemporalAdjusters.firstDayOfNextMonth());
    }

    return ranges;
  }

  TimeRange month(LocalDateTime time) {
    Month month = Month.from(time);
    Year year = Year.from(time);

    LocalDateTime startOfMonth = time.with(TemporalAdjusters.firstDayOfMonth());
    LocalDateTime endOfMonth = time.with(TemporalAdjusters.lastDayOfMonth());

    String label = month.getDisplayName(TextStyle.FULL, Locale.CANADA) + " " + year.getValue();
    return new TimeRange(label, startOfMonth.toInstant(zone).toEpochMilli(), endOfMonth.toInstant(zone).toEpochMilli(),
        zone.getId(), false, null, null, null, false);
  }
}
