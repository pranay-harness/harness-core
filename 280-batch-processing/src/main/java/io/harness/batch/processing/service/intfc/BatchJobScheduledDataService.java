package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;

import java.time.Instant;
import java.util.List;

public interface BatchJobScheduledDataService {
  boolean create(BatchJobScheduledData batchJobScheduledData);

  Instant fetchLastBatchJobScheduledTime(String accountId, BatchJobType batchJobType);

  Instant fetchLastDependentBatchJobScheduledTime(String accountId, BatchJobType batchJobType);

  void invalidateJobs(String accountId, List<String> batchJobTypes, Instant instant);
}
