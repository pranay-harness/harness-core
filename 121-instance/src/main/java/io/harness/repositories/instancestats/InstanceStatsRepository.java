package io.harness.repositories.instancestats;

import io.harness.models.InstanceStats;

public interface InstanceStatsRepository {
  InstanceStats getLatestRecord(String accountId);
}
