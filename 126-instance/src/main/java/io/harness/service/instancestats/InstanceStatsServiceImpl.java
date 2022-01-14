package io.harness.service.instancestats;

import io.harness.models.InstanceStats;
import io.harness.repositories.instancestats.InstanceStatsRepository;

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
