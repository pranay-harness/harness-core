package io.harness.repositories.instancestats;

import io.harness.pojo.InstanceStats;

public interface InstanceStatsRepository {
  InstanceStats getLatestRecord(String accountId);
}
