package io.harness.ccm.cluster.dao;

import io.harness.ccm.cluster.entities.BatchJobInterval;
import io.harness.ccm.cluster.entities.BatchJobInterval.BatchJobIntervalKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BatchJobIntervalDao {
  private final HPersistence hPersistence;

  @Inject
  public BatchJobIntervalDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType) {
    return hPersistence.createQuery(BatchJobInterval.class)
        .filter(BatchJobIntervalKeys.accountId, accountId)
        .filter(BatchJobIntervalKeys.batchJobType, batchJobType)
        .get();
  }
}
