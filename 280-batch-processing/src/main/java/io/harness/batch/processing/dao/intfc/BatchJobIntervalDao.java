/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.commons.entities.batch.BatchJobInterval;

public interface BatchJobIntervalDao {
  boolean create(BatchJobInterval batchJobInterval);
  BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType);
}
