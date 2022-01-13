/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.cluster.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.entities.batch.BatchJobInterval;
import io.harness.ccm.commons.entities.batch.BatchJobInterval.BatchJobIntervalKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public class BatchJobIntervalDao {
  private final HPersistence hPersistence;

  @Inject
  public BatchJobIntervalDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType) {
    return hPersistence.createQuery(BatchJobInterval.class)
        .filter(BatchJobIntervalKeys.accountId, accountId)
        .filter(BatchJobIntervalKeys.batchJobType, batchJobType)
        .get();
  }
}
