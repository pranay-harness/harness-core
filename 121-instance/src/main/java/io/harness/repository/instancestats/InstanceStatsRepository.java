package io.harness.repository.instancestats;

import io.harness.entity.InstanceStats;

public interface InstanceStatsRepository {
  InstanceStats getLatestRecord(String accountId);
}
