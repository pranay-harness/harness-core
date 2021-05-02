package io.harness.instancesync.service.instancestats;

import io.harness.instancesync.entity.InstanceStats;
import io.harness.instancesync.repository.instancestats.InstanceStatsRepository;

import com.google.inject.Inject;
import java.time.Instant;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceStatsServiceImpl implements InstanceStatsService {
  private InstanceStatsRepository instanceStatsRepository;

  @Override
  public Instant getLastSnapshotTime(String accountId) {
    InstanceStats record = instanceStatsRepository.getLatestRecord(accountId);
    if (record == null) {
      // no record found
      return null;
    }
    return record.getReportedAt().toInstant();
  }
}
