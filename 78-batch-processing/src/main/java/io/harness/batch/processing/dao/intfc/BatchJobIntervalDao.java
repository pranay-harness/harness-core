package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.BatchJobInterval;

public interface BatchJobIntervalDao {
  boolean create(BatchJobInterval batchJobInterval);
  BatchJobInterval fetchBatchJobInterval(String accountId, BatchJobType batchJobType);
}
