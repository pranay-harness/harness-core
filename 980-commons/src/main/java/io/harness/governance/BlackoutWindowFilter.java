package io.harness.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public interface BlackoutWindowFilter {
  BlackoutWindowFilterType getFilterType();

  void setFilterType(BlackoutWindowFilterType filterType);
}
