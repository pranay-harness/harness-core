package io.harness.service.stats.statscollector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public interface StatsCollector {
  boolean createStats(String accountId);
}
