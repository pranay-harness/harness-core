package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.BatchJobInterval;

public interface BatchJobIntervalService {
  BatchJobInterval fetchBatchJobInterval(String accountId, BatchJobType batchJobType);
}
