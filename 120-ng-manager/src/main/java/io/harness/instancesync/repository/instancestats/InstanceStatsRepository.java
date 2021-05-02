package io.harness.instancesync.repository.instancestats;

import io.harness.instancesync.entity.InstanceStats;

public interface InstanceStatsRepository {
  InstanceStats getLatestRecord(String accountId);
}
