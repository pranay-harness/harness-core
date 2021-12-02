package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.servicelevelobjective.entities.SLIRecord;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIRecordKeys;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class SLIRecordServiceImpl implements SLIRecordService {
  @Inject private HPersistence hPersistence;
  @Override
  public void create(SLIRecord sliRecord) {
    hPersistence.save(sliRecord);
  }

  private List<SLIRecord> sliRecords(Instant startTime, Instant endTime, Duration duration) {
    return hPersistence.createQuery(SLIRecord.class)
        .field(SLIRecordKeys.timestamp)
        .greaterThanOrEq(startTime)
        .field(SLIRecordKeys.timestamp)
        .lessThan(endTime)
        .field(SLIRecordKeys.epochMinute)
        .mod(duration.toMinutes(), 0)
        .asList();
  }
}
