package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;

import java.time.Instant;
import java.util.List;

public interface BatchJobScheduledDataDao {
  boolean create(BatchJobScheduledData batchJobScheduledData);

  BatchJobScheduledData fetchLastBatchJobScheduledData(String accountId, BatchJobType batchJobType);

  void invalidateJobs(String accountId, List<String> batchJobTypes, Instant instant);
}
