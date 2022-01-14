package software.wings.resources.stats.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.resources.stats.model.TimeRange;

@OwnedBy(HarnessTeam.CDC)
public interface TimeRangeChecker {
  boolean istTimeInRange(TimeRange timeRange, long currentTimeMillis);
}
