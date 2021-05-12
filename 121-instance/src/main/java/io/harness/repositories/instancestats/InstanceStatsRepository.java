package io.harness.repositories.instancestats;

import io.harness.entity.InstanceStats;

public interface InstanceStatsRepository {
  InstanceStats getLatestRecord(String accountId);
}
