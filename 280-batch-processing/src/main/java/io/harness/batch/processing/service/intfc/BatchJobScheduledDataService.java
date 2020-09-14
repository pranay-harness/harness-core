package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;

import java.time.Instant;

public interface BatchJobScheduledDataService {
  boolean create(BatchJobScheduledData batchJobScheduledData);

  Instant fetchLastBatchJobScheduledTime(String accountId, BatchJobType batchJobType);

  Instant fetchLastDependentBatchJobScheduledTime(String accountId, BatchJobType batchJobType);
}
