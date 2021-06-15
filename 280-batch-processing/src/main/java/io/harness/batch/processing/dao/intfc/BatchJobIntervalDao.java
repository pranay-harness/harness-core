package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.commons.entities.batch.BatchJobInterval;

public interface BatchJobIntervalDao {
  boolean create(BatchJobInterval batchJobInterval);
  BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType);
}
