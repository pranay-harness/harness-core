package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;
import io.harness.ccm.commons.entities.CEDataCleanupRequest;

import java.time.Instant;
import java.util.List;

public interface BatchJobScheduledDataService {
  boolean create(BatchJobScheduledData batchJobScheduledData);

  Instant fetchLastBatchJobScheduledTime(String accountId, BatchJobType batchJobType);

  Instant fetchLastDependentBatchJobScheduledTime(String accountId, BatchJobType batchJobType);

  Instant fetchLastDependentBatchJobCreatedTime(String accountId, BatchJobType batchJobType);

  void invalidateJobs(CEDataCleanupRequest ceDataCleanupRequest);

  void invalidateJobs(String accountId, List<String> batchJobTypes, Instant instant);
}
