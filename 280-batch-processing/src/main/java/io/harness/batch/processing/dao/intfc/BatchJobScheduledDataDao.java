/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.ccm.commons.entities.batch.BatchJobScheduledData;
import io.harness.ccm.commons.entities.batch.CEDataCleanupRequest;

import java.time.Instant;
import java.util.List;

public interface BatchJobScheduledDataDao {
  boolean create(BatchJobScheduledData batchJobScheduledData);

  BatchJobScheduledData fetchLastBatchJobScheduledData(String accountId, BatchJobType batchJobType);

  void invalidateJobs(CEDataCleanupRequest ceDataCleanupRequest);

  void invalidateJobs(String accountId, List<String> batchJobTypes, Instant instant);
}
