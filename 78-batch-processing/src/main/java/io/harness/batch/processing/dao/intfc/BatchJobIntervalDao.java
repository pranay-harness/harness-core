package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.cluster.entities.BatchJobInterval;

public interface BatchJobIntervalDao {
  boolean create(BatchJobInterval batchJobInterval);
  BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType);
}
