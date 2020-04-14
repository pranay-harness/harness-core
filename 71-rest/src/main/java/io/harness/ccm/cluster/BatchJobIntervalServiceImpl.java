package io.harness.ccm.cluster;

import static java.time.temporal.ChronoUnit.HOURS;

import com.google.inject.Inject;

import io.harness.ccm.cluster.dao.BatchJobIntervalDao;
import io.harness.ccm.cluster.entities.BatchJobInterval;

public class BatchJobIntervalServiceImpl implements BatchJobIntervalService {
  @Inject BatchJobIntervalDao batchJobIntervalDao;

  @Override
  public boolean isIntervalUnitHours(String accountId, String batchJobType) {
    BatchJobInterval batchJobInterval = batchJobIntervalDao.fetchBatchJobInterval(accountId, batchJobType);
    return batchJobInterval != null && batchJobInterval.getIntervalUnit() == HOURS;
  }
}
