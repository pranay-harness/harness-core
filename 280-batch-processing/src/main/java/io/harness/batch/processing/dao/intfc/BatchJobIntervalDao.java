/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.commons.entities.batch.BatchJobInterval;

public interface BatchJobIntervalDao {
  boolean create(BatchJobInterval batchJobInterval);
  BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType);
}
