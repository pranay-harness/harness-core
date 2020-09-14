package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;

public interface BatchJobScheduledDataDao {
  boolean create(BatchJobScheduledData batchJobScheduledData);

  BatchJobScheduledData fetchLastBatchJobScheduledData(String accountId, BatchJobType batchJobType);
}
