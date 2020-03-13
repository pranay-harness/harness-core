package io.harness.batch.processing.service.impl;

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

  @Override
  public boolean create(BatchJobScheduledData batchJobScheduledData) {
    return batchJobScheduledDataDao.create(batchJobScheduledData);
  }

  @Override
  public Instant fetchLastBatchJobScheduledTime(String accountId, BatchJobType batchJobType) {
    Instant instant = fetchLastDependentBatchJobScheduledTime(accountId, batchJobType);
    if (null != instant) {
      return instant;
    } else {
      Instant firstEventReceivedTime = lastReceivedPublishedMessageDao.getFirstEventReceivedTime(accountId);
      if (ImmutableSet.of(BatchJobType.ECS_EVENT, BatchJobType.K8S_EVENT).contains(batchJobType)) {
        logger.info("Changing first event received time {} {}", accountId, batchJobType);
        firstEventReceivedTime = Instant.ofEpochMilli(1577903400000l).truncatedTo(ChronoUnit.DAYS);
      }
      return firstEventReceivedTime;
    }
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
