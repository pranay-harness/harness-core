package io.harness.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.resources.stats.service.AnnualTimeRangeChecker;
import software.wings.resources.stats.service.DailyTimeRangeChecker;
import software.wings.resources.stats.service.MonthlyTimeRangeChecker;
import software.wings.resources.stats.service.TimeRangeChecker;
import software.wings.resources.stats.service.WeeklyTimeRangeChecker;

import lombok.Getter;

@OwnedBy(HarnessTeam.CDC)
public enum TimeRangeOccurrence {
  DAILY(new DailyTimeRangeChecker()),
  WEEKLY(new WeeklyTimeRangeChecker()),
  MONTHLY(new MonthlyTimeRangeChecker()),
  ANNUAL(new AnnualTimeRangeChecker());

  @Getter private TimeRangeChecker timeRangeChecker;

  TimeRangeOccurrence(TimeRangeChecker timeRangeChecker) {
    this.timeRangeChecker = timeRangeChecker;
  }
}
