package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public enum QueryAlertCategory {
  COLLSCAN,
  MANY_ENTRIES_EXAMINED,
  SORT_STAGE,
  SLOW_QUERY;
}
