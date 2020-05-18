package io.harness.batch.processing.service.impl;

import static io.harness.batch.processing.ccm.BatchJobType.ACTUAL_IDLE_COST_BILLING_HOURLY;
import static io.harness.batch.processing.ccm.BatchJobType.INSTANCE_BILLING_HOURLY;
import static io.harness.batch.processing.ccm.BatchJobType.UNALLOCATED_BILLING_HOURLY;

import com.google.common.collect.ImmutableSet;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class BatchJobScheduledDataServiceImpl implements BatchJobScheduledDataService {
  @Autowired private BatchJobScheduledDataDao batchJobScheduledDataDao;
  @Autowired protected LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  private static final int MAX_HOURLY_DATA = 16;

  @Override
  public boolean create(BatchJobScheduledData batchJobScheduledData) {
    return batchJobScheduledDataDao.create(batchJobScheduledData);
  }

  @Override
  public Instant fetchLastBatchJobScheduledTime(String accountId, BatchJobType batchJobType) {
    Instant instant = fetchLastDependentBatchJobScheduledTime(accountId, batchJobType);
    if (null == instant) {
      instant = lastReceivedPublishedMessageDao.getFirstEventReceivedTime(accountId);
    }

    // in case of hourly billing jobs getting max date as 16 days before current date
    if (ImmutableSet.of(INSTANCE_BILLING_HOURLY, ACTUAL_IDLE_COST_BILLING_HOURLY, UNALLOCATED_BILLING_HOURLY)
            .contains(batchJobType)) {
      Instant startInstant = Instant.now().minus(MAX_HOURLY_DATA, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      instant = startInstant.isAfter(instant) ? startInstant : instant;
    }
    return instant;
  }

  @Override
  public Instant fetchLastDependentBatchJobScheduledTime(String accountId, BatchJobType batchJobType) {
    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(accountId, batchJobType);
    if (null != batchJobScheduledData) {
      return batchJobScheduledData.getEndAt();
    }
    return null;
  }
}
