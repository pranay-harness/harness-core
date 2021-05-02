package io.harness.instancesync.service.instancestats;

import java.time.Instant;

public interface InstanceStatsService {
  Instant getLastSnapshotTime(String accountId);
}
